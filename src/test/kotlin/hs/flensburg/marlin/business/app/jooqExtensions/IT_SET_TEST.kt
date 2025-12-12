package hs.flensburg.marlin.business.app.jooqExtensions

import hs.flensburg.marlin.business.setIfNotNull
import hs.flensburg.marlin.business.setWhen
import hs.flensburg.marlin.core.IntegrationTest
import hs.flensburg.marlin.database.generated.tables.references.USER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IT_SET_TEST : IntegrationTest() {

    private val testUserId = 999999L
    private val testUserId2 = 999998L

    @BeforeEach
    fun setupTestData() {
        cleanupTestData()

        dsl.insertInto(USER)
            .set(USER.ID, testUserId)
            .set(USER.EMAIL, "test@example.com")
            .set(USER.FIRST_NAME, "John")
            .set(USER.LAST_NAME, "Doe")
            .execute()

        dsl.insertInto(USER)
            .set(USER.ID, testUserId2)
            .set(USER.EMAIL, "test2@example.com")
            .set(USER.FIRST_NAME, "Jane")
            .set(USER.LAST_NAME, "Doe")
            .execute()

        println("Test data created successfully")
    }

    @AfterEach
    fun cleanupTestData() {
        dsl.deleteFrom(USER)
            .where(USER.ID.`in`(testUserId, testUserId2))
            .execute()
        println("Test data cleaned up")
    }

    @Test
    fun `setIfNotNull should update field when value is not null`() {
        val newEmail = "updated@example.com"

        val rowsUpdated = dsl.update(USER)
            .setIfNotNull(USER.EMAIL, newEmail)
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val updatedUser = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(updatedUser, "User should exist")
        Assertions.assertEquals(newEmail, updatedUser?.email, "Email should be updated")
        Assertions.assertEquals("John", updatedUser?.firstName, "First name should remain unchanged")
        Assertions.assertEquals("Doe", updatedUser?.lastName, "Last name should remain unchanged")
    }

    @Test
    fun `setIfNotNull should not update field when value is null`() {
        val nullValue: String? = null
        val originalEmail = "test@example.com"

        val rowsUpdated = dsl.update(USER)
            .setIfNotNull(USER.EMAIL, nullValue)
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(0, rowsUpdated, "No rows should be updated")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(originalEmail, user?.email, "Email should not be changed when null is passed")
    }

    @Test
    fun `setIfNotNull should handle multiple fields with mixed null and non-null values`() {
        val newEmail = "newemail@example.com"
        val nullValue: String? = null
        val newLastName = "UpdatedDoe"

        val rowsUpdated = dsl.update(USER)
            .setIfNotNull(USER.EMAIL, newEmail)
            .setIfNotNull(USER.FIRST_NAME, nullValue)
            .setIfNotNull(USER.LAST_NAME, newLastName)
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated")
        Assertions.assertEquals("John", user?.firstName, "First name should remain unchanged (null was passed)")
        Assertions.assertEquals(newLastName, user?.lastName, "Last name should be updated")
    }

    @Test
    fun `setIfNotNull should work in chain with regular set`() {
        val newEmail = "chained@example.com"
        val newFirstName = "ChainedJohn"

        val rowsUpdated = dsl.update(USER)
            .set(USER.EMAIL, newEmail)
            .setIfNotNull(USER.FIRST_NAME, newFirstName)
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated")
        Assertions.assertEquals(newFirstName, user?.firstName, "First name should be updated")
    }

    @Test
    fun `setWhen should update field when predicate returns true`() {
        val newEmail = "valid@example.com"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.EMAIL, newEmail) { it?.contains("@") == true }
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated when predicate is true")
    }

    @Test
    fun `setWhen should not update field when predicate returns false`() {
        val invalidEmail = "invalid-email"
        val originalEmail = "test@example.com"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.EMAIL, invalidEmail) { it?.contains("@") == true }
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(0, rowsUpdated, "No rows should be updated")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(originalEmail, user?.email, "Email should not be updated when predicate is false")
    }

    @Test
    fun `setWhen should handle multiple fields with different predicates`() {
        val newEmail = "test@example.com"
        val newFirstName = "Alexander"
        val newLastName = "X"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.EMAIL, newEmail) { it?.contains("@") == true }
            .setWhen(USER.FIRST_NAME, newFirstName) { (it?.length ?: 0) > 2 }
            .setWhen(USER.LAST_NAME, newLastName) { (it?.length ?: 0) > 2 }
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated (passes predicate)")
        Assertions.assertEquals(newFirstName, user?.firstName, "First name should be updated (passes predicate)")
        Assertions.assertEquals("Doe", user?.lastName, "Last name should NOT be updated (fails predicate)")
    }

    @Test
    fun `setWhen should work with predicates`() {
        val newEmail = "test@example.com"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.EMAIL, newEmail) { email ->
                email?.contains("@") == true &&
                (email.length) > 5 &&
                email.endsWith(".com")
            }
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated when complex predicate is true")
    }

    @Test
    fun `setWhen should not update when predicate fails`() {
        val shortEmail = "a@b.c"
        val originalEmail = "test@example.com"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.EMAIL, shortEmail) { email ->
                email?.contains("@") == true && (email?.length ?: 0) > 10
            }
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(0, rowsUpdated, "No rows should be updated")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(originalEmail, user?.email, "Email should not be updated when complex predicate fails")
    }

    @Test
    fun `setWhen should work in chain with regular set`() {
        val newEmail = "regular@example.com"
        val newFirstName = "John"

        val rowsUpdated = dsl.update(USER)
            .set(USER.EMAIL, newEmail)
            .setWhen(USER.FIRST_NAME, newFirstName) { !it.isNullOrEmpty() }
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated")
        Assertions.assertEquals(newFirstName, user?.firstName, "First name should be updated")
    }


    @Test
    fun `setIfNotNull and setWhen should work together`() {
        val newEmail = "combined@example.com"
        val newFirstName = "Combined"
        val nullValue: String? = null

        val rowsUpdated = dsl.update(USER)
            .setIfNotNull(USER.EMAIL, newEmail)
            .setWhen(USER.FIRST_NAME, newFirstName) { (it?.length ?: 0) > 2 }
            .setIfNotNull(USER.LAST_NAME, nullValue)
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated")
        Assertions.assertEquals(newFirstName, user?.firstName, "First name should be updated")
        Assertions.assertEquals("Doe", user?.lastName, "Last name should remain unchanged (null was passed)")
    }

    @Test
    fun `both functions should maintain query builder chain and update correctly`() {
        val newEmail = "chain@example.com"
        val newFirstName = "Chain"
        val newLastName = "Builder"

        val rowsUpdated = dsl.update(USER)
            .setIfNotNull(USER.EMAIL, newEmail)
            .setWhen(USER.FIRST_NAME, newFirstName) { !it.isNullOrEmpty() }
            .set(USER.LAST_NAME, newLastName)
            .where(USER.ID.eq(testUserId))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(1, rowsUpdated, "Should update exactly 1 row")

        val user = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        Assertions.assertNotNull(user, "User should exist")
        Assertions.assertEquals(newEmail, user?.email, "Email should be updated")
        Assertions.assertEquals(newFirstName, user?.firstName, "First name should be updated")
        Assertions.assertEquals(newLastName, user?.lastName, "Last name should be updated")
    }

    @Test
    fun `functions should work with multiple row updates`() {
        val newFirstName = "BatchUpdated"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.FIRST_NAME, newFirstName) { (it?.length ?: 0) > 0 }
            .where(USER.ID.`in`(testUserId, testUserId2))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(2, rowsUpdated, "Should update exactly 2 rows")

        val users = dsl.selectFrom(USER)
            .where(USER.ID.`in`(testUserId, testUserId2))
            .fetch()

        Assertions.assertEquals(2, users.size, "Should fetch 2 users")
        Assertions.assertTrue(users.all { it.firstName == newFirstName }, "All users should have updated first name")
    }

    @Test
    fun `setWhen with always-false predicate should not update any rows`() {
        val originalEmail1 = "test@example.com"
        val originalEmail2 = "test2@example.com"

        val rowsUpdated = dsl.update(USER)
            .setWhen(USER.EMAIL, "willnotupdate@example.com") { false }
            .where(USER.ID.`in`(testUserId, testUserId2))
            .execute()

        println("Rows updated: $rowsUpdated")
        Assertions.assertEquals(0, rowsUpdated, "No rows should be updated")

        val user1 = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId))
            .fetchOne()

        val user2 = dsl.selectFrom(USER)
            .where(USER.ID.eq(testUserId2))
            .fetchOne()

        Assertions.assertNotNull(user1, "User 1 should exist")
        Assertions.assertNotNull(user2, "User 2 should exist")
        Assertions.assertEquals(originalEmail1, user1?.email, "User 1 email should not be updated")
        Assertions.assertEquals(originalEmail2, user2?.email, "User 2 email should not be updated")
    }
}