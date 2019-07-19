package info.laht.threekt.renderers.opengl

import info.laht.threekt.core.*
import org.lwjgl.opengl.GL15

class GLGeometries(
    private val attributes: GLAttributes,
    private val info: GLInfo
) {

    private val onGeometryDispose = OnGeometryDispose()
    private val geometries = mutableMapOf<Int, BufferGeometry>()
    private val wireframeAttributes = mutableMapOf<Int, BufferAttribute>()


    fun get(`object`: Object3D, geometry: BufferGeometry): BufferGeometry {
        var buffergeometry = geometries[geometry.id];

        if (buffergeometry != null) return buffergeometry;

        geometry.addEventListener("dispose", onGeometryDispose);

        buffergeometry = geometry;

        geometries[geometry.id] = buffergeometry;

        info.memory.geometries++;

        return buffergeometry;
    }

    fun update(geometry: BufferGeometry) {
        val index = geometry.index;
        val geometryAttributes = geometry.attributes;

        if (index != null) {

            attributes.update(index, GL15.GL_ELEMENT_ARRAY_BUFFER);

        }

        for ((name, attr) in geometryAttributes) {

            attributes.update(attr, GL15.GL_ARRAY_BUFFER);

        }

        // morph targets

//        val morphAttributes = geometry.morphAttributes;
//
//        for ( var name in morphAttributes ) {
//
//            var array = morphAttributes[ name ];
//
//            for ( var i = 0, l = array.length; i < l; i ++ ) {
//
//                attributes.update( array[ i ], gl.ARRAY_BUFFER );
//
//            }
//
//        }
    }

    fun getWireframeAttribute(geometry: BufferGeometry): BufferAttribute {
        var attribute = wireframeAttributes[geometry.id]

        if (attribute != null) return attribute;

        val indices = mutableListOf<Int>()

        val geometryIndex = geometry.index;
        val geometryAttributes = geometry.attributes;

        if (geometryIndex != null) {

            val array = geometryIndex.array;

            for (i in 0 until array.size step 3) {

                val a = array[i + 0];
                val b = array[i + 1];
                val c = array[i + 2];

                indices.add(a);
                indices.add(b);
                indices.add(b);
                indices.add(c);
                indices.add(c);
                indices.add(a);

            }

        } else {

            val array = geometryAttributes.position?.array
                ?: throw IllegalStateException("No position attribute found!")


            for (i in 0 until (array.size / 3) - 1 step 3) {

                val a = i + 0;
                val b = i + 1;
                val c = i + 2;

                indices.add(a);
                indices.add(b);
                indices.add(b);
                indices.add(c);
                indices.add(c);
                indices.add(a);

            }

        }

        attribute = IntBufferAttribute(indices.toIntArray(), 1)

        attributes.update(attribute, GL15.GL_ELEMENT_ARRAY_BUFFER);

        wireframeAttributes[geometry.id] = attribute;

        return attribute;

    }

    inner class OnGeometryDispose : EventLister {

        override fun onEvent(event: Event) {

            val geometry = event.target as BufferGeometry
            val buffergeometry =
                geometries[geometry.id] ?: throw IllegalStateException("Not a valid key: ${geometry.id}!")

            buffergeometry.index?.also {
                attributes.remove(it)
            }

            buffergeometry.attributes.values.forEach {
                attributes.remove(it)
            }

            geometry.removeEventListener("dispose", onGeometryDispose)
            geometries.remove(geometry.id)

            wireframeAttributes[buffergeometry.id]?.also {
                attributes.remove(it)
                wireframeAttributes.remove(buffergeometry.id)
            }

            info.memory.geometries--

        }
    }

}
