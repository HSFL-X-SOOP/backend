package hs.flensburg.marlin.business.integration

import hs.flensburg.marlin.business.OrderBy
import hs.flensburg.marlin.database.generated.tables.references.USER_VIEW
import hs.flensburg.marlin.core.IntegrationTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class SortIntegrationTest : IntegrationTest() {

    @Test
    fun `test Sort toSortField with actual database query - ascending`() {
        val sort = OrderBy("email", ascending = true)
        val sortField = sort.toOrderField()

        val query = dsl.selectFrom(USER_VIEW)
            .orderBy(sortField)
            .limit(10)

        val sql = query.getSQL()
        println("Executing query: $sql")

        val results = query.fetch()
        println("Query executed successfully, fetched ${results.size} rows")

        assertTrue(sql.contains("order by"), "SQL should contain ORDER BY")
        assertTrue(sql.lowercase().contains("email"), "SQL should sort by email")
    }

    @Test
    fun `test Sort toSortField with actual database query - descending`() {
        val sort = OrderBy("user_created_at", ascending = false)
        val sortField = sort.toOrderField()

        val query = dsl.selectFrom(USER_VIEW)
            .orderBy(sortField)
            .limit(10)

        val sql = query.getSQL()
        println("Executing query: $sql")

        val results = query.fetch()
        println("Query executed successfully, fetched ${results.size} rows")

        assertTrue(sql.contains("order by"), "SQL should contain ORDER BY")
        assertTrue(sql.lowercase().contains("user_created_at"), "SQL should sort by created_at")
        assertTrue(sql.lowercase().contains("desc"), "SQL should use DESC ordering")
    }

    @Test
    fun `test multiple Sort fields with actual database query`() {
        val primarySort = OrderBy("user_created_at", ascending = false)
        val secondarySort = OrderBy("email", ascending = true)

        val query = dsl.selectFrom(USER_VIEW)
            .orderBy(primarySort.toOrderField(), secondarySort.toOrderField())
            .limit(10)

        val sql = query.getSQL()
        println("Executing query: $sql")

        val results = query.fetch()
        println("Query executed successfully, fetched ${results.size} rows")

        assertTrue(sql.contains("order by"), "SQL should contain ORDER BY")
        assertTrue(sql.lowercase().contains("user_created_at"), "SQL should sort by created_at")
        assertTrue(sql.lowercase().contains("email"), "SQL should sort by email")
    }

    @Test
    fun `test Sort with pagination`() {
        val sort = OrderBy("email", ascending = true)

        val query = dsl.selectFrom(USER_VIEW)
            .orderBy(sort.toOrderField())
            .limit(5)
            .offset(0)

        val sql = query.getSQL()
        println("Executing paginated query: $sql")

        val results = query.fetch()
        println("Query executed successfully, fetched ${results.size} rows")

        assertTrue(results.size <= 5, "Should fetch at most 5 rows")
    }
}
