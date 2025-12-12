package hs.flensburg.marlin.business.app.sort

import hs.flensburg.marlin.business.OrderBy
import hs.flensburg.marlin.database.generated.tables.references.USER_VIEW
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class UNIT_SORT_TEST {

    @Test
    fun `test Sort toSortField with ascending order`() {
        val sort = OrderBy("email", ascending = true)
        val sortField = sort.toOrderField()

        val sqlString = sortField.toString()
        Assertions.assertTrue(sqlString.contains("email"), "Sort field should contain 'email'")
        Assertions.assertTrue(sqlString.contains("asc"), "Sort field should be ascending")

        println("Ascending sort SQL: $sqlString")
    }

    @Test
    fun `test Sort toSortField with descending order`() {
        val sort = OrderBy("CREATED_AT", ascending = false)
        val sortField = sort.toOrderField()

        val sqlString = sortField.toString()
        Assertions.assertTrue(sqlString.contains("CREATED_AT"), "Sort field should contain 'created_at'")
        Assertions.assertTrue(sqlString.contains("desc"), "Sort field should be descending")

        println("Descending sort SQL: $sqlString")
    }

    @Test
    fun `test Sort parse with asc modifier`() {
        val sort = OrderBy.Companion.parse("email.asc")

        Assertions.assertEquals("email", sort.orderBy, "Sort field should be 'EMAIL'")
        Assertions.assertEquals(true, sort.ascending, "Sort should be ascending")
    }

    @Test
    fun `test Sort parse with desc modifier`() {
        val sort = OrderBy.Companion.parse("created_at.desc")

        Assertions.assertEquals("created_at", sort.orderBy, "Sort field should be 'CREATED_AT'")
        Assertions.assertEquals(false, sort.ascending, "Sort should be descending")
    }

    @Test
    fun `test Sort parse without modifier defaults to asc`() {
        val sort = OrderBy.Companion.parse("username")

        Assertions.assertEquals("username", sort.orderBy, "Sort field should be 'username'")
        Assertions.assertEquals(true, sort.ascending, "Sort should default to ascending")
    }

    @Test
    fun `test Sort parse with invalid modifier defaults to asc`() {
        val sort = OrderBy.Companion.parse("email.invalid")

        Assertions.assertEquals("email", sort.orderBy, "Sort field should be 'EMAIL'")
        Assertions.assertEquals(true, sort.ascending, "Invalid modifier should default to ascending")
    }

    @Test
    fun `test Sort toSortField generates valid jOOQ OrderField`() {
        val sortAsc = OrderBy("email", ascending = true)
        val sortDesc = OrderBy("ID", ascending = false)

        val sortFieldAsc = sortAsc.toOrderField()
        val sortFieldDesc = sortDesc.toOrderField()

        val query = DSL.using(SQLDialect.POSTGRES)
            .selectFrom(USER_VIEW)
            .orderBy(sortFieldAsc, sortFieldDesc)

        val sql = query.getSQL()
        println("Generated SQL query: $sql")

        Assertions.assertTrue(sql.contains("order by"), "Query should contain ORDER BY clause")
        Assertions.assertTrue(sql.contains("email"), "Query should sort by email")
        Assertions.assertTrue(sql.contains("id"), "Query should sort by id")
    }

    @Test
    fun `test multiple Sort fields in query`() {
        val primarySort = OrderBy("created_at", ascending = false)
        val secondarySort = OrderBy("email", ascending = true)

        val primaryField = primarySort.toOrderField()
        val secondaryField = secondarySort.toOrderField()

        val query = DSL.using(SQLDialect.POSTGRES)
            .selectFrom(USER_VIEW)
            .orderBy(primaryField, secondaryField)

        val sql = query.getSQL()
        println("Multi-sort SQL query: $sql")

        Assertions.assertTrue(sql.contains("created_at"), "Query should include primary sort field")
        Assertions.assertTrue(sql.contains("email"), "Query should include secondary sort field")
    }

    @Test
    fun `test Sort with special characters in field name`() {
        val sort = OrderBy("user_activity_role", ascending = true)
        val sortField = sort.toOrderField()

        val sqlString = sortField.toString()
        Assertions.assertTrue(sqlString.contains("user_activity_role"), "Sort field should handle underscores")

        println("Sort with underscores SQL: $sqlString")
    }
}