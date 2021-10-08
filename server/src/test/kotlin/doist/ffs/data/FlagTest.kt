package doist.ffs.data

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import doist.ffs.capturingLastInsertId
import doist.ffs.getDatabase
import doist.ffs.withDatabase
import kotlin.test.Test
import kotlin.test.assertFails

internal class FlagTest {
    private var projectId: Long = -1
    private val testDriver: SqlDriver
        get() {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            val database = getDatabase(driver)
            val organizationId = database.capturingLastInsertId {
                organizationQueries.insert(name = "test-name")
            }
            projectId = database.capturingLastInsertId {
                projectQueries.insert(organization_id = organizationId, name = "test-name")
            }
            return driver
        }

    @Test
    fun testInsertValid(): Unit = withDatabase(testDriver) { db ->
        val name = "test-name"
        val rule = "true"
        db.flagQueries.insert(project_id = projectId, name = name, rule = rule)
        val flag = db.flagQueries.selectByProject(projectId).executeAsList()[0]
        assert(flag.project_id == projectId)
        assert(flag.name == name)
    }

    @Test
    fun testInsertDuplicatedName(): Unit = withDatabase(testDriver) { db ->
        val name = "test-name"
        val rule = "true"
        db.flagQueries.insert(project_id = projectId, name = name, rule = rule)
        assertFails {
            db.flagQueries.insert(project_id = projectId, name = name, rule = rule)
        }
    }

    @Test
    fun testSelectByOrganization(): Unit = withDatabase(testDriver) { db ->
        val namePrefix = "test-name-"
        val rule = "true"
        for (i in 0..9) {
            db.flagQueries.insert(project_id = projectId, name = "$namePrefix-$i", rule = rule)
        }
        val flags = db.flagQueries.selectByProject(projectId).executeAsList()
        assert(flags.size == 10)
        flags.forEachIndexed { index, flag ->
            assert(flag.name.startsWith(namePrefix))
            assert(flags.subList(index + 1, flags.size).none { flag.name == it.name })
        }
    }

    @Test
    fun testSelect(): Unit = withDatabase(testDriver) { db ->
        val name = "test-name"
        val rule = "true"
        val id = db.capturingLastInsertId {
            flagQueries.insert(project_id = projectId, name = name, rule = rule)
        }
        val flag = db.flagQueries.select(id).executeAsOne()
        assert(flag.project_id == projectId)
        assert(flag.name == name)
    }

    @Test
    fun testUpdateRule(): Unit = withDatabase(testDriver) { db ->
        val name = "test-name"
        val oldRule = "true"
        val newRule = "false"
        val id = db.capturingLastInsertId {
            flagQueries.insert(project_id = projectId, name = name, rule = oldRule)
        }
        db.flagQueries.updateRule(id = id, rule = newRule)
        val project = db.flagQueries.select(id).executeAsOne()
        assert(project.rule == newRule)
    }

    @Test
    fun testDelete(): Unit = withDatabase(testDriver) { db ->
        val name = "test-name"
        val rule = "true"
        val id = db.capturingLastInsertId {
            flagQueries.insert(project_id = projectId, name = name, rule = rule)
        }
        db.flagQueries.delete(id)
        val project = db.flagQueries.select(id).executeAsOneOrNull()
        assert(project == null)
    }
}
