package info.laht.threekt.materials

import info.laht.threekt.core.Uniform
import info.laht.threekt.renderers.shaders.ShaderChunk

open class ShaderMaterial : Material(), MaterialWithSkinning, MaterialWithMorphTarget, MaterialWithMorphNormals {

    var type = "ShaderMaterial"

    var uniforms: Map<String, Uniform> = emptyMap()
        internal set

    var vertexShader = ShaderChunk.default_vertex
    var fragmentShader = ShaderChunk.default_fragment

    var linewidth = 1f

    var wireframe = false
    var wireframeLinewidth = 1f

    var clipping = false // set to use user-defined clipping planes

    override var skinning = false // set to use skinning attribute streams
    override var morphTargets = false // set to use morph targets
    override var morphNormals = false // set to use morph normals

    val extensions = Extensions()

    var uniformsNeedUpdate = false

    init {

        fog = false // set to use scene fog
        lights = false // set to use scene lights

    }

    fun copy(source: ShaderMaterial): ShaderMaterial {

        super.copy(source)

        this.fragmentShader = source.fragmentShader
        this.vertexShader = source.vertexShader

        this.wireframe = source.wireframe
        this.wireframeLinewidth = source.wireframeLinewidth

        this.lights = source.lights
        this.clipping = source.clipping

        this.skinning = source.skinning

        this.morphTargets = source.morphTargets
        this.morphNormals = source.morphNormals

        this.extensions.copy(source.extensions)

        return this
    }

    class Extensions(
        var derivatives: Boolean = false,
        var fragDepth: Boolean = false,
        var drawBuffers: Boolean = false,
        var shaderTextureLOD: Boolean = false
    ) {
        fun copy(source: Extensions): Extensions {
            this.derivatives = source.derivatives
            this.fragDepth = source.fragDepth
            this.drawBuffers = source.drawBuffers
            this.shaderTextureLOD = source.shaderTextureLOD

            return this
        }
    }

}

class RawShaderMaterial : ShaderMaterial()