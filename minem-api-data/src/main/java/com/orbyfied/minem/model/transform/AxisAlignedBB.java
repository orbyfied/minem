package com.orbyfied.minem.model.transform;

import com.orbyfied.minem.math.Mutates;
import com.orbyfied.minem.math.Vec3d;
import lombok.*;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class AxisAlignedBB {

    public double minX, minY, minZ; // always the lowest x, y and z values
    public double maxX, maxY, maxZ; // always the highest x, y and z values

    /**
     * Creates a bounding box with the given corners, verifies that x1 < x2, y1 < y2, z1 < z2 unlike
     * the unsafe, direct constructor.
     */
    public static AxisAlignedBB of(double x1, double y1, double z1,
                                   double x2, double y2, double z2) {
        if (x1 > x2) { double t = x2; x2 = x1; x1 = t; }
        if (y1 > y2) { double t = y2; y2 = y1; y1 = t; }
        if (z1 > z2) { double t = z2; z2 = z1; z1 = t; }

        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Create an axis aligned bounding box with the given width and height,
     * horizontally centered on and vertically anchored, on the very bottom, by
     * the given position vector.
     */
    public static AxisAlignedBB ofCenterBase(Vec3d pos, double width, double height) {
        double hw = width / 2;
        return new AxisAlignedBB(
                pos.x - hw, pos.y, pos.z - hw,
                pos.x + hw, pos.y + height, pos.z + hw
        );
    }

    public double sizeX() {
        return maxX - minX;
    }

    public double sizeZ() {
        return maxZ - minZ;
    }

    public double sizeY() {
        return maxY - minY;
    }

    public Vec3d low() {
        return new Vec3d(minX, minY, minZ);
    }

    public Vec3d high() {
        return new Vec3d(maxX, maxY, maxZ);
    }

    /**
     * Get the horizontally centered and vertically, on the very bottom, anchored vector of this bounding box.
     */
    public Vec3d centerBase() {
        return new Vec3d(
                (minX + maxX) / 2,
                minY,
                (minZ + maxZ) / 2
        );
    }

    public AxisAlignedBB offset(double dx, double dy, double dz) {
        return new AxisAlignedBB(
                minX + dx, minY + dy, minZ + dz,
                maxX + dx, maxY + dy, maxZ + dz
        );
    }

    @Mutates
    public AxisAlignedBB offsetMutable(double dx, double dy, double dz) {
        this.minX += dx; this.maxX += dx;
        this.minY += dy; this.maxY += dy;
        this.minZ += dz; this.maxZ += dz;
        return this;
    }

    public AxisAlignedBB expanded(double x, double y, double z) {
        return new AxisAlignedBB(
                this.minX - x, this.minY - y, this.minZ - z,
                this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    @Mutates
    public AxisAlignedBB expand(double x, double y, double z) {
        this.minX = this.minX - x; this.maxX = this.maxX + x;
        this.minY = this.minY - y; this.maxY = this.maxY + y;
        this.minZ = this.minZ - z; this.maxZ = this.maxZ + z;
        return this;
    }

    public boolean intersects(AxisAlignedBB other) {
        return this.maxX > other.minX &&
                this.minX < other.maxX &&
                this.maxY > other.minY &&
                this.minY < other.maxY &&
                this.maxZ > other.minZ &&
                this.minZ < other.maxZ;
    }

    public boolean intersectsHorizontally(AxisAlignedBB other) {
        return this.maxX > other.minX &&
                this.minX < other.maxX &&
                this.maxZ > other.minZ &&
                this.minZ < other.maxZ;
    }

    public boolean contains(Vec3d p) {
        return p.x >= minX && p.x <= maxX &&
                p.y >= minY && p.y <= maxY &&
                p.z >= minZ && p.z <= maxZ;
    }

    public boolean equalsEpsilon(AxisAlignedBB other, double eps) {
        return Math.abs(minX - other.minX) < eps &&
                Math.abs(minY - other.minY) < eps &&
                Math.abs(minZ - other.minZ) < eps &&
                Math.abs(maxX - other.maxX) < eps &&
                Math.abs(maxY - other.maxY) < eps &&
                Math.abs(maxZ - other.maxZ) < eps;
    }

    public AxisAlignedBB set(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        this.minX = minX; this.maxX = maxX;
        this.minY = minY; this.maxY = maxY;
        this.minZ = minZ; this.maxZ = maxZ;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AxisAlignedBB that)) return false;
        return Double.compare(minX, that.minX) == 0 && Double.compare(minY, that.minY) == 0 && Double.compare(minZ, that.minZ) == 0 && Double.compare(maxX, that.maxX) == 0 && Double.compare(maxY, that.maxY) == 0 && Double.compare(maxZ, that.maxZ) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String toString() {
        return "AABB(" + minX + ", " + minY + ", " + minZ + " : " + maxX + ", " + maxY + ", " + maxZ + ")";
    }

}
