package info.laht.threekt.cameras

import info.laht.threekt.core.Object3DImpl
import info.laht.threekt.math.Matrix4
import info.laht.threekt.math.Vector3

open class Camera : Object3DImpl() {

    val matrixWorldInverse = Matrix4()

    val projectionMatrix = Matrix4()
    val projectionMatrixInverse = Matrix4()


    override fun getWorldDirection(target: Vector3): Vector3 {

        this.updateMatrixWorld(true)

        val e = this.matrixWorld.elements

        return target.set(-e[8], -e[9], -e[10]).normalize()

    }

    override fun updateMatrixWorld(force: Boolean) {

        super.updateMatrixWorld(force)

        this.matrixWorldInverse.getInverse(this.matrixWorld)

    }

    fun copy(source: Camera, recursive: Boolean): Camera {

        super.copy(source, recursive)

        this.matrixWorldInverse.copy(source.matrixWorldInverse)

        this.projectionMatrix.copy(source.projectionMatrix)
        this.projectionMatrixInverse.copy(source.projectionMatrixInverse)

        return this

    }

    override fun clone(): Camera {
        return Camera().copy(this, true)
    }

}

fun Vector3.project(camera: Camera): Vector3 {
    return this.applyMatrix4(camera.matrixWorldInverse).applyMatrix4(camera.projectionMatrix)
}

fun Vector3.unproject(camera: Camera): Vector3 {
    return this.applyMatrix4(camera.projectionMatrixInverse).applyMatrix4(camera.matrixWorld)
}

