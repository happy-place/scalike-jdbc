package com.scalike.jdbc.h2

import java.time.ZonedDateTime
import scalikejdbc._
import scalikejdbc.config._


case class Member(id: Long, name: Option[String], createdAt: ZonedDateTime, updatedAt: ZonedDateTime)

object Member extends SQLSyntaxSupport[Member] {
  override val tableName = "members"

  def apply(rs: WrappedResultSet): Member = new Member(
    rs.long("id"), rs.stringOpt("name"),
    rs.zonedDateTime("created_at"),
    rs.zonedDateTime("updated_at")
  )
}

case class Programmer(id:Long,name:String,companyId:Long,createdAt:ZonedDateTime,updatedAt:ZonedDateTime)
case class Company(id:Long,name:String,createdAt:ZonedDateTime,updatedAt:ZonedDateTime)

object Programmer extends SQLSyntaxSupport[Programmer] {
  override def tableName: String = "program"
  def apply(programRS:WrappedResultSet,companyRS:WrappedResultSet): Programmer = new Programmer(id, name, companyId, createdAt, updatedAt)
}

