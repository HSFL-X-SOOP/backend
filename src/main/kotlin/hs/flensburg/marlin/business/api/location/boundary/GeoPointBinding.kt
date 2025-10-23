package hs.flensburg.marlin.business.api.location.boundary

import org.jooq.*
import org.jooq.impl.DSL
import java.sql.*
import org.postgresql.util.PGobject


class GeoPointBinding : Binding<Any, GeoPoint> {

    override fun converter(): Converter<Any, GeoPoint> = Converter.ofNullable(
        Any::class.java,
        GeoPoint::class.java,
        { dbObj ->
            when (dbObj) {
                is PGobject -> parsePGobject(dbObj)
                is String -> parseWKT(dbObj)
                else -> null
            }
        },
        { gp -> gp?.let { "SRID=4326;POINT(${it.lon} ${it.lat})" } }
    )

    private fun parsePGobject(obj: PGobject): GeoPoint? {
        return parseWKT(obj.value ?: return null)
    }

    private fun parseWKT(value: String): GeoPoint? {
        // Check if it's WKB hex format (starts with hex chars like "0101...")
        if (value.matches(Regex("^[0-9A-Fa-f]+$"))) {
            return parseWKB(value)
        }
        
        // Otherwise parse as WKT: "SRID=4326;POINT(lon lat)"
        val coords = value.substringAfter("POINT(")
            .substringBefore(")")
            .trim()
            .split(" ")
        if (coords.size != 2) return null
        val lon = coords[0].toDoubleOrNull()
        val lat = coords[1].toDoubleOrNull()
        return if (lat != null && lon != null) GeoPoint(lat, lon) else null
    }

    private fun parseWKB(hexString: String): GeoPoint? {
        try {
            val bytes = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val buffer = java.nio.ByteBuffer.wrap(bytes)
            
            // Read byte order (1 = little endian, 0 = big endian)
            val byteOrder = buffer.get()
            if (byteOrder == 1.toByte()) {
                buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            }
            
            // Read geometry type (should be 0x20000001 for POINT with SRID)
            val geomType = buffer.getInt()
            
            // Read SRID if present (bit 0x20000000 is set)
            if ((geomType and 0x20000000) != 0) {
                buffer.getInt() // Skip SRID
            }
            
            // Read coordinates (lon, lat)
            val lon = buffer.getDouble()
            val lat = buffer.getDouble()
            
            return GeoPoint(lat, lon)
        } catch (e: Exception) {
            return null
        }
    }

    // --- Binding SQL contexts ---
    override fun sql(ctx: BindingSQLContext<GeoPoint>) {
        val value = ctx.value()
        if (value == null) {
            ctx.render().sql("NULL")
        } else {
            ctx.render()
                .visit(DSL.sql("ST_SetSRID(ST_MakePoint({0}, {1}), 4326)", value.lon, value.lat))
        }
    }

    override fun register(ctx: BindingRegisterContext<GeoPoint>) {
        ctx.statement().registerOutParameter(ctx.index(), Types.OTHER)
    }

    override fun set(ctx: BindingSetStatementContext<GeoPoint>) {
        val value = ctx.value()
        if (value == null) {
            ctx.statement().setNull(ctx.index(), Types.OTHER)
        } else {
            val obj = org.postgresql.util.PGobject().apply {
                type = "geography"
                this.value = "SRID=4326;POINT(${value.lon} ${value.lat})"
            }
            ctx.statement().setObject(ctx.index(), obj)
        }
    }

    override fun get(ctx: BindingGetResultSetContext<GeoPoint>) {
        val obj = ctx.resultSet().getObject(ctx.index())
        ctx.value(converter().from(obj))
    }

    override fun get(ctx: BindingGetStatementContext<GeoPoint>) {
        val obj = ctx.statement().getObject(ctx.index())
        ctx.value(converter().from(obj))
    }

    override fun set(ctx: BindingSetSQLOutputContext<GeoPoint>) {
        throw SQLFeatureNotSupportedException()
    }

    override fun get(ctx: BindingGetSQLInputContext<GeoPoint>) {
        throw SQLFeatureNotSupportedException()
    }
}
