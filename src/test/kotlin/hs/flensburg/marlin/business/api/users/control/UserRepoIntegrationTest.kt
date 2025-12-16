package hs.flensburg.marlin.business.api.users.control

import hs.flensburg.marlin.core.IntegrationTest
import hs.flensburg.marlin.database.generated.enums.Language
import hs.flensburg.marlin.database.generated.enums.MeasurementSystem
import hs.flensburg.marlin.database.generated.enums.UserActivityRole
import hs.flensburg.marlin.database.generated.enums.UserAuthorityRole
import hs.flensburg.marlin.database.generated.tables.pojos.HarborMasterLocation
import hs.flensburg.marlin.database.generated.tables.pojos.User
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import hs.flensburg.marlin.database.generated.tables.references.HARBOR_MASTER_LOCATION
import hs.flensburg.marlin.database.generated.tables.references.LOCATION
import hs.flensburg.marlin.database.generated.tables.references.USER
import hs.flensburg.marlin.database.generated.tables.references.USER_PROFILE
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepoIntegrationTest : IntegrationTest() {

    private var testLocationId: Long = 0
    private var testLocation2Id: Long = 0
    private var adminUserId: Long = 0

    @BeforeAll
    fun setupTestData() {
        testLocationId = dsl.insertInto(LOCATION)
            .set(LOCATION.NAME, "Test Harbor 1")
            .returningResult(LOCATION.ID)
            .fetchOne()!!
            .value1()!!

        testLocation2Id = dsl.insertInto(LOCATION)
            .set(LOCATION.NAME, "Test Harbor 2")
            .returningResult(LOCATION.ID)
            .fetchOne()!!
            .value1()!!

        adminUserId = dsl.insertInto(USER)
            .set(USER.EMAIL, "admin@test.com")
            .set(USER.PASSWORD, "hashedpassword")
            .set(USER.ROLE, UserAuthorityRole.ADMIN)
            .set(USER.VERIFIED, true)
            .set(USER.FIRST_NAME, "Admin")
            .set(USER.LAST_NAME, "User")
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, adminUserId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.SAILOR))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()
    }

    @AfterAll
    fun cleanupTestData() {
        dsl.deleteFrom(USER).where(USER.EMAIL.like("%@test.com")).execute()
        dsl.deleteFrom(LOCATION).where(LOCATION.ID.`in`(testLocationId, testLocation2Id)).execute()
    }

    @BeforeEach
    fun cleanupUsers() {
        dsl.deleteFrom(USER)
            .where(USER.EMAIL.like("testuser%@test.com"))
            .execute()
    }

    @Test
    fun `test update - change role from HARBOR_MASTER to USER should delete harbor_master_location`() {
        val userRecord = UserRecord().apply {
            email = "testuser1@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.HARBOR_MASTER
            verified = true
            firstName = "John"
            lastName = "Doe"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.SAILOR))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        dsl.insertInto(HARBOR_MASTER_LOCATION)
            .set(HARBOR_MASTER_LOCATION.USER_ID, userId)
            .set(HARBOR_MASTER_LOCATION.LOCATION_ID, testLocationId)
            .set(HARBOR_MASTER_LOCATION.ASSIGNED_BY, adminUserId)
            .execute()

        val harborMasterLocationBefore = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNotNull(harborMasterLocationBefore, "Harbor master location should exist before role change")
        assertEquals(testLocationId, harborMasterLocationBefore!!.locationId, "Location should be assigned")

        val updatedUser = !UserRepo.update(
            userId = userId,
            firstName = "John",
            lastName = "Doe",
            authorityRole = UserAuthorityRole.USER,
            verified = true,
            locationId = null
        )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals(UserAuthorityRole.USER, updatedUser!!.role)

        val harborMasterLocationAfter = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNull(harborMasterLocationAfter, "Harbor master location record should be deleted after role change from HARBOR_MASTER")
    }

    @Test
    fun `test update - change role from HARBOR_MASTER to ADMIN should delete harbor_master_location`() {
        val userRecord = UserRecord().apply {
            email = "testuser2@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.HARBOR_MASTER
            verified = true
            firstName = "Jane"
            lastName = "Smith"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.DE)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.FISHERMAN))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        dsl.insertInto(HARBOR_MASTER_LOCATION)
            .set(HARBOR_MASTER_LOCATION.USER_ID, userId)
            .set(HARBOR_MASTER_LOCATION.LOCATION_ID, testLocationId)
            .set(HARBOR_MASTER_LOCATION.ASSIGNED_BY, adminUserId)
            .execute()

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "Jane",
                lastName = "Smith",
                authorityRole = UserAuthorityRole.ADMIN,
                verified = true,
                locationId = null
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals(UserAuthorityRole.ADMIN, updatedUser!!.role)

        val harborMasterLocation = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNull(harborMasterLocation, "Harbor master location should be deleted after role change to ADMIN")
    }

    @Test
    fun `test update - keep HARBOR_MASTER role and update location`() {
        val userRecord = UserRecord().apply {
            email = "testuser3@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.HARBOR_MASTER
            verified = true
            firstName = "Bob"
            lastName = "Johnson"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.SWIMMER))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        dsl.insertInto(HARBOR_MASTER_LOCATION)
            .set(HARBOR_MASTER_LOCATION.USER_ID, userId)
            .set(HARBOR_MASTER_LOCATION.LOCATION_ID, testLocationId)
            .set(HARBOR_MASTER_LOCATION.ASSIGNED_BY, adminUserId)
            .execute()

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "Bob",
                lastName = "Johnson",
                authorityRole = UserAuthorityRole.HARBOR_MASTER,
                verified = true,
                locationId = testLocation2Id
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals(UserAuthorityRole.HARBOR_MASTER, updatedUser!!.role)

        val harborMasterLocation = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNotNull(harborMasterLocation, "Harbor master location should not be null")
        assertEquals(testLocation2Id, harborMasterLocation!!.locationId, "Location should be updated to new location")
    }

    @Test
    fun `test update - change role from USER to HARBOR_MASTER should create harbor_master_location`() {
        val userRecord = UserRecord().apply {
            email = "testuser4@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.USER
            verified = true
            firstName = "Alice"
            lastName = "Williams"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.SAILOR))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        val harborMasterLocationBefore = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNull(harborMasterLocationBefore, "Harbor master location should not exist before role change")

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "Alice",
                lastName = "Williams",
                authorityRole = UserAuthorityRole.HARBOR_MASTER,
                verified = true,
                locationId = testLocationId
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals(UserAuthorityRole.HARBOR_MASTER, updatedUser!!.role)

        val harborMasterLocation = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNotNull(harborMasterLocation, "Harbor master location should be created")
        assertEquals(testLocationId, harborMasterLocation!!.locationId, "Location should be set when changing to HARBOR_MASTER")
    }

    @Test
    fun `test update - keep HARBOR_MASTER role without updating location`() {
        val userRecord = UserRecord().apply {
            email = "testuser5@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.HARBOR_MASTER
            verified = true
            firstName = "Charlie"
            lastName = "Brown"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.FISHERMAN))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        dsl.insertInto(HARBOR_MASTER_LOCATION)
            .set(HARBOR_MASTER_LOCATION.USER_ID, userId)
            .set(HARBOR_MASTER_LOCATION.LOCATION_ID, testLocationId)
            .set(HARBOR_MASTER_LOCATION.ASSIGNED_BY, adminUserId)
            .execute()

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "Charlie",
                lastName = "Brown",
                authorityRole = UserAuthorityRole.HARBOR_MASTER,
                verified = true,
                locationId = null
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals(UserAuthorityRole.HARBOR_MASTER, updatedUser!!.role)

        val harborMasterLocation = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNotNull(harborMasterLocation, "Harbor master location should still exist")
        assertEquals(testLocationId, harborMasterLocation!!.locationId, "Location should remain unchanged when locationId is null")
    }

    @Test
    fun `test update - keep USER role should not affect harbor_master_location`() {
        val userRecord = UserRecord().apply {
            email = "testuser6@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.USER
            verified = false
            firstName = "David"
            lastName = "Miller"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.DE)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.SWIMMER))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.IMPERIAL)
            .execute()

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "David",
                lastName = "Miller",
                authorityRole = UserAuthorityRole.USER,
                verified = true,
                locationId = null
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals(UserAuthorityRole.USER, updatedUser!!.role)
        assertEquals("David", updatedUser.firstName)
        assertTrue(updatedUser.verified == true)

        val harborMasterLocation = dsl.selectFrom(HARBOR_MASTER_LOCATION)
            .where(HARBOR_MASTER_LOCATION.USER_ID.eq(userId))
            .fetchOneInto(HarborMasterLocation::class.java)

        assertNull(harborMasterLocation, "No harbor master location should exist for USER role")
    }

    @Test
    fun `test update - update first and last name`() {
        val userRecord = UserRecord().apply {
            email = "testuser7@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.USER
            verified = true
            firstName = "OldFirst"
            lastName = "OldLast"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.FISHERMAN))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "NewFirst",
                lastName = "NewLast",
                authorityRole = UserAuthorityRole.USER,
                verified = true,
                locationId = null
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals("NewFirst", updatedUser!!.firstName)
        assertEquals("NewLast", updatedUser.lastName)
    }

    @Test
    fun `test update - blank names should not update`() {
        val userRecord = UserRecord().apply {
            email = "testuser8@test.com"
            password = "hashedpassword"
            role = UserAuthorityRole.USER
            verified = true
            firstName = "OriginalFirst"
            lastName = "OriginalLast"
        }

        val userId = dsl.insertInto(USER)
            .set(userRecord)
            .returningResult(USER.ID)
            .fetchOne()!!
            .value1()!!

        dsl.insertInto(USER_PROFILE)
            .set(USER_PROFILE.USER_ID, userId)
            .set(USER_PROFILE.LANGUAGE, Language.EN)
            .set(USER_PROFILE.ROLE, arrayOf(UserActivityRole.SAILOR))
            .set(USER_PROFILE.MEASUREMENT_SYSTEM, MeasurementSystem.METRIC)
            .execute()

        val updatedUser = !UserRepo.update(
                userId = userId,
                firstName = "",
                lastName = "  ",
                authorityRole = UserAuthorityRole.USER,
                verified = true,
                locationId = null
            )

        assertNotNull(updatedUser, "Updated user should not be null")
        assertEquals("OriginalFirst", updatedUser!!.firstName, "First name should not be updated with blank value")
        assertEquals("OriginalLast", updatedUser.lastName, "Last name should not be updated with blank value")
    }
}
