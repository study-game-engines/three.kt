package info.laht.threekt.objects

import info.laht.threekt.core.*
import info.laht.threekt.materials.LineBasicMaterial
import info.laht.threekt.math.Vector3
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import info.laht.threekt.materials.MaterialProxy

open class Line @JvmOverloads constructor(

    geometry: BufferGeometry? = null,
    material: LineBasicMaterial? = null

) : Object3D(), BufferGeometryProxy, MaterialProxy {

    override var geometry = geometry ?: BufferGeometry()
    override var material = material ?: LineBasicMaterial()

    open fun computeLineDistances(): Line {

        val start = Vector3()
        val end = Vector3()

        // we assume non-indexed geometry
        if (geometry.index == null) {

            val positionAttribute = geometry.attributes.position!!
            val lineDistances = mutableListOf(0f)

            for (i in 1 until positionAttribute.count) {
                positionAttribute.toVector3(i - 1, start)
                positionAttribute.toVector3(i, end)

                lineDistances[i] = lineDistances[i - 1]
                lineDistances[i] += start.distanceTo(end)
            }

            geometry.addAttribute("lineDistance", FloatBufferAttribute(lineDistances.toFloatArray(), 1))

        } else {

            LOG.warn("computeLineDistances(): Computation only possible with non-indexed BufferGeometry.")

        }

        return this
    }

    override fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>) {
        TODO()
    }

    fun copy(source: Line): Line {
        super.copy(source, false)

        this.geometry.copy(source.geometry)
        this.material.copy(source.material)

        return this
    }

    override fun clone(): Line {
        return Line(this.geometry, this.material).copy(this)
    }

    private companion object {

        val LOG: Logger = LoggerFactory.getLogger(Line::class.java)

    }

}
