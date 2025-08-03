package hs.flensburg.marlin.business.api.users.control

import de.lambda9.tailwind.jooq.JIO
import de.lambda9.tailwind.jooq.Jooq
import hs.flensburg.marlin.database.generated.tables.pojos.User
import hs.flensburg.marlin.database.generated.tables.records.UserRecord
import hs.flensburg.marlin.database.generated.tables.references.USER

object UserRepo {
    fun insert(user: UserRecord): JIO<User> = Jooq.query {
        insertInto(USER)
            .set(user)
            .returning()
            .fetchInto(User::class.java).first()
    }

    fun fetchById(id: Long): JIO<User?> = Jooq.query {
        selectFrom(USER)
            .where(USER.ID.eq(id))
            .fetchOneInto(User::class.java)
    }

    fun setEmailIsVerified(id: Long): JIO<Unit> = Jooq.query {
        update(USER)
            .set(USER.VERIFIED, true)
            .where(USER.ID.eq(id))
            .execute()
    }

    fun fetchByEmail(email: String): JIO<User?> = Jooq.query {
        selectFrom(USER)
            .where(USER.EMAIL.eq(email))
            .fetchOneInto(User::class.java)
    }
}