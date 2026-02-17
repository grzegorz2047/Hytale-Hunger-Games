package pl.grzegorz2047.hytale.hungergames.arena.border;

import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.concurrent.ThreadLocalRandom;

public class ArenaBorder {
    private final Vector3d borderCenterLocation;
    private final String  borderWorldName;
    private double arenaBorderSize;

    public ArenaBorder(String borderWorldName, Vector3d borderCenterLocation, double arenaBorderSize) {
        this.borderWorldName = borderWorldName;
        this.borderCenterLocation = borderCenterLocation.clone();
        this.borderCenterLocation.setY(0);
        this.arenaBorderSize = arenaBorderSize;
    }

    public void shrink() {
        if(arenaBorderSize > 10) {
            arenaBorderSize -= 1;
        }
    }
    private double getArenaBorderSize() {
        return arenaBorderSize;
    }

    private Vector3f getBorderColor() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
//        return new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
        return new Vector3f((float) 31 /255, (float) 219 /255, (float) 191 /255);
    }

    public void refresh() {
        double scale = getArenaBorderSize() * 2;
        Matrix4d tmp = new Matrix4d();
        Matrix4d matrix = new Matrix4d();
        matrix.identity();
        matrix.translate(borderCenterLocation.x, borderCenterLocation.y, borderCenterLocation.z);
        matrix.scale(scale, 100, scale);
//        matrix.rotateAxis(0, (double)0.0F, (double)1.0F, (double)0.0F, tmp);
//        DebugUtils.addSector(getArenaWorld(),borderCenterLocation.x, borderCenterLocation.y, borderCenterLocation.z, 0,scale, 0, getBorderColor(),10, false);
        float time = 10;
        DebugUtils.add(getArenaWorld(), DebugShape.Sphere, matrix, getBorderColor(), time, false);
    }

    private World getArenaWorld() {
        return Universe.get().getWorld(borderWorldName);
    }

    private float getArenaBorderLifeTime() {
        return 1;
    }

    public boolean isOutOfBound(Vector3d position) {
        return !isInsideBorder(position);
    }

    private boolean isInsideBorder(Vector3d position) {
        return position.distanceTo(borderCenterLocation) <= arenaBorderSize + 2;
    }
}
