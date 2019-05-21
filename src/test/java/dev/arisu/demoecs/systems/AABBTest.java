package dev.arisu.demoecs.systems;

import org.junit.Assert;
import org.junit.Test;

public class AABBTest {

    public static final float EPS = 0.00000001f;

    @Test
    public void testIntersectsIfInside() {
        final AABB outer = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        final AABB inner = new AABB(0.2f, 0.2f, 0.2f, 0.8f, 0.8f, 0.8f);

        Assert.assertTrue(AABB.intersects(outer, inner));
        Assert.assertTrue(AABB.intersects(inner, outer));
    }

    @Test
    public void testIntersectsIfDisjoint() {
        final AABB first = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        final AABB second = new AABB(0.0f, 2.0f, 0.0f, 1.0f, 3.0f, 1.0f);

        Assert.assertFalse(AABB.intersects(first, second));
        Assert.assertFalse(AABB.intersects(second, first));
    }

    @Test
    public void testIntersects() {
        final AABB first = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        final AABB second = new AABB(0.0f, 0.5f, 0.0f, 1.0f, 1.5f, 1.0f);

        Assert.assertTrue(AABB.intersects(first, second));
        Assert.assertTrue(AABB.intersects(second, first));
    }

    @Test
    public void testIntersectsIfTouching() {
        final AABB first = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        final AABB second = new AABB(0.0f, 1.0f, 0.0f, 1.0f, 2.0f, 1.0f);

        Assert.assertFalse(AABB.intersects(first, second));
        Assert.assertFalse(AABB.intersects(second, first));

        final AABB first2 = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        final AABB second2 = new AABB(1.0f, 0.0f, 0.0f, 2.0f, 1.0f, 1.0f);

        Assert.assertFalse(AABB.intersects(first2, second2));
        Assert.assertFalse(AABB.intersects(second2, first2));

        final AABB first3 = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        final AABB second3 = new AABB(1.0f, 1.0f, 0.0f, 2.0f, 2.0f, 1.0f);

        Assert.assertFalse(AABB.intersects(first3, second3));
        Assert.assertFalse(AABB.intersects(second3, first3));
    }

    @Test
    public void testIntersectsIfSame() {
        final AABB first = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);

        Assert.assertTrue(AABB.intersects(first, first));
    }

    @Test
    public void testExpandPositive() {
        final AABB subject = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)
                .expand(0.2f, 0.2f, 0.2f);

        Assert.assertEquals(0.0f, subject.getMinX(), EPS);
        Assert.assertEquals(0.0f, subject.getMinY(), EPS);
        Assert.assertEquals(0.0f, subject.getMinZ(), EPS);
        Assert.assertEquals(1.2f, subject.getMaxX(), EPS);
        Assert.assertEquals(1.2f, subject.getMaxY(), EPS);
        Assert.assertEquals(1.2f, subject.getMaxZ(), EPS);
    }

    @Test
    public void testExpandNegative() {
        final AABB subject = new AABB(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)
                .expand(-0.2f, -0.2f, -0.2f);

        Assert.assertEquals(-0.2f, subject.getMinX(), EPS);
        Assert.assertEquals(-0.2f, subject.getMinY(), EPS);
        Assert.assertEquals(-0.2f, subject.getMinZ(), EPS);
        Assert.assertEquals(1.0f, subject.getMaxX(), EPS);
        Assert.assertEquals(1.0f, subject.getMaxY(), EPS);
        Assert.assertEquals(1.0f, subject.getMaxZ(), EPS);
    }
}
