package doist.ffs.ext

import com.squareup.sqldelight.ColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import doist.ffs.Database
import doist.ffs.db.Flag
import doist.ffs.db.Organization
import doist.ffs.db.Project
import kotlinx.datetime.Instant

val Database.organizations get() = organizationQueries
val Database.projects get() = projectQueries
val Database.flags get() = flagQueries

/**
 * Runs [block], typically an insert, and returns the id of the last inserted row.
 */
fun Database.capturingLastInsertId(block: Database.() -> Unit) =
    transactionWithResult<Long> {
        block()
        internalQueries.lastInsertId().executeAsOne()
    }

/**
 * Initializes a [Database] instance for [driver].
 */
fun Database(driver: SqlDriver): Database {
    // Create or migrate database.
    val oldVersion =
        driver.executeQuery(null, "PRAGMA user_version", 0)
            .takeIf { it.next() }?.getLong(0)?.toInt() ?: 0
    val newVersion = Database.Schema.version
    if (oldVersion == 0) {
        Database.Schema.create(driver)
        driver.execute(null, "PRAGMA user_version=$newVersion", 0)
    } else if (oldVersion < newVersion) {
        Database.Schema.migrate(driver, oldVersion, newVersion)
        driver.execute(null, "PRAGMA user_version=$newVersion", 0)
    }
    // Enable foreign key support.
    driver.execute(null, "PRAGMA foreign_keys=ON", 0)
    // Instantiate and return database.
    return Database(
        driver = driver,
        organizationAdapter = Organization.Adapter(instantAdapter, instantAdapter),
        projectAdapter = Project.Adapter(instantAdapter, instantAdapter),
        flagAdapter = Flag.Adapter(instantAdapter, instantAdapter),
    )
}

private val instantAdapter = object : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long) = Instant.fromEpochSeconds(databaseValue, 0)
    override fun encode(value: Instant) = value.epochSeconds
}

/**
 * Database file path. When blank, an in-memory database is used.
 */
private var databaseDriver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
var Database.driver: SqlDriver
    get() = databaseDriver
    set(value) {
        databaseDriver = value
        database = Database(databaseDriver)
    }

/**
 * Singleton [Database] object.
 */
var database: Database = Database(databaseDriver)
    private set