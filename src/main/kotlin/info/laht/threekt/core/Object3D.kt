package info.laht.threekt.core

import info.laht.threekt.cameras.Camera
import info.laht.threekt.lights.Light
import info.laht.threekt.materials.Material
import info.laht.threekt.materials.MeshDepthMaterial
import info.laht.threekt.materials.MeshDistanceMaterial
import info.laht.threekt.math.*
import info.laht.threekt.objects.Group
import info.laht.threekt.renderers.GLRenderer
import info.laht.threekt.scenes.Scene
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate

interface GeometryObject {
    val geometry: BufferGeometry
}

interface MaterialObject {
    val material: Material
}

open class Object3D : Cloneable, EventDispatcher() {

    var name = ""
    val uuid = generateUUID()
    val id = object3DId.getAndIncrement()

    var parent: Object3D? = null
    val children = mutableListOf<Object3D>()

    var up = defaultUp.clone()

    val position = Vector3()
    val rotation = Euler().also {
        it.onChangeCallback = { onQuaternionChange() }
    }
    val quaternion = Quaternion().also {
        it.onChangeCallback = { onRotationChange() }
    }
    val scale = Vector3(1.toFloat(), 1.toFloat(), 1.toFloat())

    val modelViewMatrix = Matrix4()
    val normalMatrix = Matrix3()

    val matrix = Matrix4()
    val matrixWorld = Matrix4()

    var matrixAutoUpdate = true
    var matrixWorldNeedsUpdate = true

    val layers = Layers()
    var visible = true

    var castShadow = true
    var receiveShadow = true

    var frustumCulled = true
    var renderOrder = 0

    var customDepthMaterial: MeshDepthMaterial? = null
    var customDistanceMaterial: MeshDistanceMaterial? = null

    internal var onBeforeRender: ((GLRenderer, Scene, Camera, BufferGeometry, Material, Group) -> Unit)? = null
    internal var onAfterRender: ((GLRenderer, Scene, Camera, BufferGeometry, Material, Group) -> Unit)? = null

    private fun onRotationChange() {
        quaternion.setFromEuler(rotation, false)
    }

    private fun onQuaternionChange() {
        rotation.setFromQuaternion(quaternion, null, false)
    }

    /**
     * This updates the position, rotation and scale with the matrix.
     */
    fun applyMatrix(matrix: Matrix4) {
        if (this.matrixAutoUpdate) {
            this.updateMatrix()
        }

        this.matrix.premultiply(matrix);

        this.matrix.decompose(this.position, this.quaternion, this.scale);
    }

    fun applyQuaternion(q: Quaternion): Object3D {
        this.quaternion.premultiply(q);

        return this;
    }

    fun setRotationFromAxisAngle(axis: Vector3, angle: Float) {
        // assumes axis is normalized
        this.quaternion.setFromAxisAngle(axis, angle);
    }

    fun setRotationFromEuler(euler: Euler) {
        this.quaternion.setFromEuler(euler, true);
    }

    fun setRotationFromMatrix(m: Matrix4) {
        // assumes the upper 3x3 of m is a pure rotation matrix (i.e, unscaled)
        this.quaternion.setFromRotationMatrix(m)
    }

    fun setRotationFromQuaternion(q: Quaternion) {
        // assumes q is normalized
        this.quaternion.copy(q)
    }

    /**
     * Rotate an object along an axis in object space. The axis is assumed to be normalized.
     * @param axis    A normalized vector in object space.
     * @param angle    The angle in radians.
     */
    fun rotateOnAxis(axis: Vector3, angle: Float): Object3D {
        // rotate object on axis in object space
        // axis is assumed to be normalized
        val q1 = Quaternion()
        q1.setFromAxisAngle(axis, angle)
        this.quaternion.multiply(q1)

        return this
    }

    /**
     * Rotate an object along an axis in world space. The axis is assumed to be normalized. Method Assumes no rotated parent.
     * @param axis    A normalized vector in object space.
     * @param angle    The angle in radians.
     */
    fun rotateOnWorldAxis(axis: Vector3, angle: Float): Object3D {
        // rotate object on axis in world space
        // axis is assumed to be normalized
        // method assumes no rotated parent
        val q1 = Quaternion()
        q1.setFromAxisAngle(axis, angle)
        this.quaternion.premultiply(q1)

        return this
    }

    /**
     *
     * @param angle
     */
    fun rotateX(angle: Float): Object3D {
        return this.rotateOnAxis(Vector3.X, angle)
    }

    /**
     *
     * @param angle
     */
    fun rotateY(angle: Float): Object3D {
        return this.rotateOnAxis(Vector3.Y, angle)
    }

    /**
     *
     * @param angle
     */
    fun rotateZ(angle: Float): Object3D {
        return this.rotateOnAxis(Vector3.Z, angle)
    }

    /**
     * @param axis    A normalized vector in object space.
     * @param distance    The distance to translate.
     */
    fun translateOnAxis(axis: Vector3, distance: Float): Object3D {
        val v1 = Vector3()
        v1.copy(axis).applyQuaternion(this.quaternion);
        this.position.add(v1.multiplyScalar(distance));

        return this;
    }

    /**
     * Translates object along x axis by distance.
     * @param distance Distance.
     */
    fun translateX(distance: Float): Object3D {
        return this.translateOnAxis(Vector3.X, distance);
    }

    /**
     * Translates object along y axis by distance.
     * @param distance Distance.
     */
    fun translateY(distance: Float): Object3D {
        return this.translateOnAxis(Vector3.Y, distance);
    }

    /**
     * Translates object along z axis by distance.
     * @param distance Distance.
     */
    fun translateZ(distance: Float): Object3D {
        return this.translateOnAxis(Vector3.Z, distance);
    }

    /**
     * Updates the vector from local space to world space.
     * @param vector A local vector.
     */
    fun localToWorld(vector: Vector3): Vector3 {
        return vector.applyMatrix4(this.matrixWorld);
    }

    /**
     * Updates the vector from world space to local space.
     * @param vector A world vector.
     */
    fun worldToLocal(vector: Vector3): Vector3 {
        return vector.applyMatrix4(Matrix4().getInverse(this.matrixWorld));
    }

    /**
     * Rotates object to face point in space.
     * @param vector A world vector to look at.
     */
    fun lookAt(v: Vector3) {
        lookAt(v.x, v.y, v.z)
    }

    /**
     * Rotates object to face point in space.
     */
    fun lookAt(x: Float, y: Float, z: Float) {
        // This method does not support objects having non-uniformly-scaled parent(s)

        val q1 = Quaternion()
        val m1 = Matrix4()
        val target = Vector3()
        val position = Vector3()

        target.set(x, y, z);

        val parent = this.parent;

        this.updateWorldMatrix(true, false);

        position.setFromMatrixPosition(this.matrixWorld);

        if (this is Camera || this is Light) {
            m1.lookAt(position, target, this.up);
        } else {
            m1.lookAt(target, position, this.up);
        }

        this.quaternion.setFromRotationMatrix(m1);

        if (parent != null) {

            m1.extractRotation(parent.matrixWorld);
            q1.setFromRotationMatrix(m1);
            this.quaternion.premultiply(q1.inverse());

        }

    }

    /**
     * Adds object as child of Object3D object.
     */
    fun add(vararg objects: Object3D): Object3D {

        objects.forEach {

            it.parent?.remove(it)
            it.parent = this
            children.add(it)
            it.dispatchEvent("added", this)
        }

        return this
    }

    /**
     * Removes object as child of Object3D object.
     */
    fun remove(vararg objects: Object3D): Object3D {

        objects.forEach {
            if (children.remove(it)) {
                it.parent = null
                it.dispatchEvent("removed", this)
            }
        }

        return this
    }

    /**
     * Adds object as a child of Object3D, while maintaining the object's world transform.
     */
    fun attach(`object`: Object3D): Object3D {

        val m = Matrix4()

        this.updateWorldMatrix(updateParents = true, updateChildren = false)

        m.getInverse(this.matrixWorld)

        `object`.parent?.also {
            it.updateWorldMatrix(updateParents = true, updateChildren = false);

            m.multiply(it.matrixWorld);
        }


        `object`.applyMatrix(m)
        `object`.updateWorldMatrix(updateParents = false, updateChildren = false)

        this.add(`object`)

        return this;
    }

    /**
     * Searches through the object's children and returns the first with a matching id.
     * @param id    Unique Float of the object instance
     */
    fun getObjectById(id: Int): Object3D? {
        return getObject(Predicate {
            it.id == id
        })
    }

    /**
     * Searches through the object's children and returns the first with a matching name.
     * @param name    String to match to the children's Object3d.name property.
     */
    fun getObjectByName(name: String): Object3D? {
        return getObject(Predicate {
            it.name == name
        })
    }

    fun getObject(predicate: Predicate<Object3D>): Object3D? {

        if (predicate.test(this)) {
            return this
        }

        for (i in 0 until children.size) {
            val child = children[i]
            val `object` = child.getObject(predicate)
            if (`object` != null) {
                return `object`
            }
        }
        return null
    }

    fun getWorldPosition(target: Vector3): Vector3 {
        this.updateMatrixWorld(true)

        return target.setFromMatrixPosition(this.matrixWorld)
    }

    fun getWorldQuaternion(target: Quaternion): Quaternion {
        this.updateMatrixWorld(true)

        this.matrixWorld.decompose(position, target, scale)

        return target
    }

    fun getWorldScale(target: Vector3): Vector3 {
        this.updateMatrixWorld(true)

        this.matrixWorld.decompose(position, quaternion, target)

        return target
    }

    open fun getWorldDirection(target: Vector3): Vector3 {
        this.updateMatrixWorld(true)

        val e = this.matrixWorld.elements

        return target.set(e[8], e[9], e[10]).normalize()
    }

    open fun raycast(raycaster: Raycaster, intersects: List<Intersection>) {
        // empty
    }

    fun traverse(callback: (Object3D) -> Unit) {
        callback(this)
        children.forEach {
            it.traverse { callback }
        }
    }

    fun traverseVisible(callback: (Object3D) -> Unit) {
        if (!this.visible) {
            return
        }
        callback(this)
        children.forEach {
            it.traverseVisible(callback)
        }
    }

    fun traverseAncestors(callback: (Object3D) -> Unit) {
        parent?.also {
            callback(it)
            it.traverseAncestors(callback)
        }
    }

    /**
     * Updates local transform.
     */
    fun updateMatrix() {
        this.matrix.compose(this.position, this.quaternion, this.scale)
        this.matrixWorldNeedsUpdate = true
    }

    /**
     * Updates global transform of the object and its children.
     */
    open fun updateMatrixWorld(force: Boolean = false) {

        if (this.matrixAutoUpdate) {
            this.updateMatrix()
        }

        @Suppress("NAME_SHADOWING")
        var force = force
        val parent = this.parent

        if (this.matrixWorldNeedsUpdate || force) {

            if (parent == null) {
                this.matrixWorld.copy(this.matrix)
            } else {
                this.matrixWorld.multiplyMatrices(parent.matrixWorld, this.matrix)
            }

            this.matrixWorldNeedsUpdate = false
            force = true

        }

        this.children.forEach {
            it.updateMatrixWorld(force)
        }

    }

    @JvmOverloads
    fun updateWorldMatrix(updateParents: Boolean = false, updateChildren: Boolean = false) {

        val parent = this.parent

        if (updateParents && parent != null) {
            parent.updateWorldMatrix(true, false);
        }

        if (this.matrixAutoUpdate) {
            this.updateMatrix()
        }

        if (parent == null) {
            this.matrixWorld.copy(this.matrix);
        } else {
            this.matrixWorld.multiplyMatrices(parent.matrixWorld, this.matrix);
        }

        if (updateChildren) {
            this.children.forEach {
                it.updateWorldMatrix(false, true)
            }
        }

    }

    override fun clone(): Object3D {
        return Object3D().copy(this, true)
    }

    open fun copy(source: Object3D, recursive: Boolean): Object3D {
        this.name = source.name

        this.up.copy( source.up )

        this.position.copy( source.position )
        this.quaternion.copy( source.quaternion )
        this.scale.copy( source.scale )

        this.matrix.copy( source.matrix )
        this.matrixWorld.copy( source.matrixWorld )

        this.matrixAutoUpdate = source.matrixAutoUpdate
        this.matrixWorldNeedsUpdate = source.matrixWorldNeedsUpdate

        this.layers.mask = source.layers.mask
        this.visible = source.visible

        this.castShadow = source.castShadow
        this.receiveShadow = source.receiveShadow

        this.frustumCulled = source.frustumCulled
        this.renderOrder = source.renderOrder

        if ( recursive ) {
            children.forEach { child ->
                this.add( child.clone() )
            }
        }

        return this
    }

    companion object {

        private val object3DId = AtomicInteger()

        var defaultUp = Vector3.Z.clone()

    }

}
