/*
 * Copyright (c) 2023-2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.cryptography.testtool.server.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import java.nio.file.*
import kotlin.io.path.*

internal const val DB_BATCH_SIZE = 64

internal fun Database(path: Path): Database = Database.connect(
    driver = "org.sqlite.JDBC",
    url = "jdbc:sqlite:${path.absolutePathString()}",
)

private val tables = listOf(
    Actors,
    Metadatas,
    Datas
)

// TODO: may be replace UUID ids in tables with string ids

internal fun Database.initializeTables(): Database {
    transaction(this) {
        SchemaUtils.createMissingTablesAndColumns(*tables.toTypedArray())
    }
    return this
}

internal fun mergeDatabases(folder: Path, outputName: String): Path {
    val outputPath = folder.resolve("${outputName}.db")
    val inputs = folder.listDirectoryEntries().map(::Database)
    val output = Database(outputPath).initializeTables()
    inputs.forEach { input ->
        tables.forEach { table ->
            table.copy(input, output)
        }
    }

    return outputPath
}

private fun <T : Table> T.copy(input: Database, output: Database) {
    val values = transaction(input) { selectAll().toList() }
    transaction(output) {
        batchInsert(values, shouldReturnGeneratedValues = false) { row ->
            this@copy.fields.forEach {
                @Suppress("UNCHECKED_CAST") val column = it as Column<Any>
                this[column] = row[column]
            }
        }
    }
}
