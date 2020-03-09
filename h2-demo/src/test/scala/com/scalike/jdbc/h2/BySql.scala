package com.scalike.jdbc.h2

import java.time._

import org.junit.Test
//import org.joda.time._
import scala.util._ //Try
import scalikejdbc.TxBoundary.Try._
import scalikejdbc._
import scalikejdbc.config._


class BySql {

  @Test
  def autoSession() {
    // 日志控制
    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
      enabled = true, // 启用日志
      singleLineMode = true, // 单行记录日志
      printUnprocessedStackTrace = false, //
      stackTraceDepth = 15, // 堆栈深度
      logLevel = 'debug, // 日志级别
      warningEnabled = true,
      warningThresholdMillis = 3000L, // 每毫秒处理行数
      warningLogLevel = 'warn,
      maxColumnSize = Some(100), // 最长列数
      maxBatchParamSize = Some(20) // execute 操作最大批次
    )

    // 创建连接 DB_CLOSE_DELAY 停机立刻清空
    Class.forName("org.h2.Driver")
    //    val url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"

    val url = "jdbc:h2:file:/Users/huhao/softwares/h2"
    ConnectionPool.singleton(url, "test", "test")
    implicit val session = AutoSession // 自动提交session

    // 删除表
    sql"drop table if exists members".execute().apply()

    // 必须执行 import scalikejdbc._
    sql"""
         |create table if not exists members (
         |  id serial not null primary key comment '主键',
         |  name varchar(64) unique comment '名称',
         |  created_at timestamp default current_timestamp comment '创建时间',
         |  updated_at timestamp default current_timestamp on update current_timestamp comment '更新时间'
         |)
         |""".stripMargin.execute.apply()

    // 插入
    Seq("Alice", "Bob", "Chris").foreach { name =>
      sql"insert into members(name) values (${name})".update.apply()
    }

    // 查找后将 ResultSet 自动映射为set
    val entities: List[Map[String, Any]] = sql"select * from members".map(_.toMap).list.apply()
    println(entities)
    /**
     * List(Map(ID -> 1, NAME -> Alice, CREATED_AT -> 2020-03-07 18:30:55.931),
     * Map(ID -> 2, NAME -> Bob, CREATED_AT -> 2020-03-07 18:30:55.955),
     * Map(ID -> 3, NAME -> Chris, CREATED_AT -> 2020-03-07 18:30:55.956))
     */

    // 自动通过提取器封装
    val members: List[Member] = sql"select * from members".map(rs => Member(rs)).list.apply()
    println(members)
    /**
     * List(Member(1,Some(Alice),2020-03-07T18:30:55.931+08:00[Asia/Shanghai]),
     * Member(2,Some(Bob),2020-03-07T18:30:55.955+08:00[Asia/Shanghai]),
     * Member(3,Some(Chris),2020-03-07T18:30:55.956+08:00[Asia/Shanghai]))
     */

    // 将case class 映射为表 m
    val m = Member.syntax("m")
    val name = "Alice"

    val alice: Option[Member] = withSQL {
      // 查 Member 表，并取别名为 m, 方便引用，条件为 m.name=name
      select.from(Member as m).where.eq(m.name, name)
    }.map(rs => Member(rs)).single.apply() // 取一个

    println(alice)

  }

  @Test
  def readOnly(): Unit = {
    // 只配置了一个库时，直接使用 DBs.setup() 下面直接使用 DB.readOnly 调用
    DBs.setup('h2)
    DBs.loadGlobalSettings() // 等效于上面的 LoggingSQLAndTimeSettings
    val DB = NamedDB('h2)

    // 禁止session 自动关闭，可以复用,否则只能查询一次
    DB.autoClose(false)
    try {
      val name = "Alice"
      val id: Option[Long] = DB.readOnly { implicit session =>
        sql"select id from members where name=${name}"
          .map(rs => rs.long("id"))
          .single()
          .apply()
      }

      println(id.getOrElse(0))

      val alice: Option[Member] = DB.readOnly { implicit session =>
        sql"select * from members where name=${name}"
          .map(rs => Member(rs))
          .single()
          .apply()
      }
      DB.close()

      println(alice)

    } catch {
      case t: Throwable => t.printStackTrace()
    } finally {
      DBs.closeAll()
    }
  }

  @Test
  def localTx(): Unit = {
    try {
      DBs.setup('h2)
      DBs.loadGlobalSettings() // 等效于上面的 LoggingSQLAndTimeSettings
      val DB = NamedDB('h2)
      DB.autoClose(false)
      DB.localTx { implicit session =>
        Seq("A4", "A5", "A6").map { name =>
          sql"insert into members(name) values(${name})".update.apply()
        }
      }
      val members:List[Member] = DB.readOnly { implicit session =>
        sql"select * from members".map(rs => Member(rs)).list().apply()
      }
      println(members)
      DB.close()
    } catch {
      case e:Throwable => e.printStackTrace()
    } finally {
      DBs.closeAll()
    }
  }






}

