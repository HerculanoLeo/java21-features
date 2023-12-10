package com.herculanoleo.processor;

import com.herculanoleo.models.exception.InvalidShapeException;
import com.herculanoleo.models.shape.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/*
    Pattern Matching for Switch
    SequencedCollection
*/

public class ProcessorShape {

    public static final ProcessorShape shared = new ProcessorShape();

    protected final static MathContext precision = new MathContext(3, RoundingMode.HALF_EVEN);

    public ShapeType typeOf(final Shape shape) {
        return switch (shape) {
            case CircleShape circle -> circleTypeOf(circle);
            case RectangleShape rect -> rectTypeOf(rect);
            case TriangleShape triangle -> triangleTypeOf(triangle);
            case null -> throw new InvalidShapeException();
        };
    }

    public BigDecimal distanceOf(final CartesianPoint point1, final CartesianPoint point2) {
        var dx = point2.x().subtract(point1.x()).pow(2);
        var dy = point2.y().subtract(point1.y()).pow(2);
        return dx.add(dy).sqrt(precision);
    }

    public List<BigDecimal> distancesOf(final Shape shape) {
        var cartesianPoints = shape.cartesianPoints();

        var rages = new ArrayList<BigDecimal>(cartesianPoints.size());

        if (cartesianPoints.size() > 2) {
            var it = cartesianPoints.iterator();

            var point1 = it.next();

            while (it.hasNext()) {
                var point2 = it.next();
                rages.add(distanceOf(point1, point2));
                point1 = point2;
            }
        }

        rages.add(distanceOf(cartesianPoints.getLast(), cartesianPoints.getFirst()));

        return rages;
    }

    public BigDecimal areaOf(final Shape shape) {
        return switch (shape) {
            case CircleShape circle -> areaCircleOf(circle);
            case RectangleShape rect -> areaRectOf(rect);
            case TriangleShape triangle -> areaTriangleOf(triangle);
            case null -> throw new InvalidShapeException();
        };
    }

    public BigDecimal angleOf(final CartesianPoint a, final CartesianPoint b, final CartesianPoint c) {
        var vectorAB = vectorOf(a, b);
        var vectorAC = vectorOf(a, c);

        var vecProduct = vectorAB.x().multiply(vectorAC.x()).add(vectorAB.y().multiply(vectorAC.y()));

        var distanceAB = distanceOf(a, b);
        var distanceAC = distanceOf(a, c);

        var disProduct = distanceAB.multiply(distanceAC);

        var angle = vecProduct.divide(disProduct, precision);

        var acos = Math.acos(angle.doubleValue());

        return BigDecimal.valueOf(Math.toDegrees(acos)).setScale(2, RoundingMode.HALF_EVEN);
    }

    public List<BigDecimal> anglesOf(final Shape shape) {
        var cartesianPoints = shape.cartesianPoints();

        var angles = new ArrayList<BigDecimal>(cartesianPoints.size());

        for (var i = 0; i < cartesianPoints.size(); i++) {
            if (i == 0) {
                var angle = angleOf(cartesianPoints.get(i), cartesianPoints.get(i + 1), cartesianPoints.getLast());
                angles.add(angle);
            } else if (i < (cartesianPoints.size() - 1)) {
                var angle = angleOf(cartesianPoints.get(i), cartesianPoints.get(i + 1), cartesianPoints.get(i - 1));
                angles.add(angle);
            } else {
                var angle = angleOf(cartesianPoints.get(i), cartesianPoints.getFirst(), cartesianPoints.get(i - 1));
                angles.add(angle);
            }
        }

        return angles;
    }

    protected ShapeType circleTypeOf(final CircleShape shape) {
        if (shape.cartesianPoints().stream().allMatch(Objects::nonNull)) {
            return ShapeType.CIRCLE;
        }

        throw new InvalidShapeException();
    }

    protected Set<Long> countEqualsDistances(final Collection<BigDecimal> ranges) {
        var rangesCount = new HashSet<Long>();

        for (var range : ranges) {
            var count = ranges.stream().filter(rangeAlt -> range.compareTo(rangeAlt) == 0).count();
            rangesCount.add(count);
        }

        return rangesCount;
    }

    protected ShapeType triangleTypeOf(final TriangleShape shape) {
        if (shape.cartesianPoints().stream().allMatch(Objects::nonNull)) {
            var distances = distancesOf(shape);

            var countSet = countEqualsDistances(distances);

            if (countSet.stream().allMatch(count -> Objects.equals(3L, count))) {
                return ShapeType.EQUILATERAL_TRIANGLE;
            } else if (countSet.stream().anyMatch(count -> Objects.equals(2L, count))) {
                return ShapeType.ISOSCELES_TRIANGLE;
            } else if (countSet.stream().allMatch(count -> Objects.equals(1L, count))) {
                return ShapeType.SCALENE_TRIANGLE;
            }
        }

        throw new InvalidShapeException();
    }

    protected ShapeType rectTypeOf(final RectangleShape shape) {
        if (shape.cartesianPoints().stream().allMatch(Objects::nonNull)) {
            var ranges = distancesOf(shape);

            var countSet = countEqualsDistances(ranges);

            if (countSet.stream().allMatch(count -> Objects.equals(4L, count))) {
                return ShapeType.SQUARE;
            } else if (countSet.stream().allMatch(count -> Objects.equals(2L, count))) {
                return ShapeType.RECTANGLE;
            }
        }

        throw new InvalidShapeException();
    }

    protected BigDecimal areaCircleOf(final CircleShape shape) {
        var radios = distanceOf(shape.a(), shape.b());

        var pi = BigDecimal.valueOf(Math.PI);

        return pi.multiply(radios.pow(2), precision);
    }

    protected BigDecimal areaRectOf(final RectangleShape shape) {
        var side1 = distanceOf(shape.a(), shape.b());
        var side2 = distanceOf(shape.b(), shape.c());
        return side1.multiply(side2, precision);
    }

    protected BigDecimal areaTriangleOf(final TriangleShape shape) {
        var distances = distancesOf(shape);
        var semiPerimeter = distances.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(2), precision);

        var computed = distances.stream()
                .map(semiPerimeter::subtract)
                .reduce(BigDecimal.ZERO, (accumulator, distance) -> {
                    if (BigDecimal.ZERO.compareTo(accumulator) == 0) {
                        return distance;
                    }
                    return accumulator.multiply(distance);
                }).multiply(semiPerimeter, precision);

        return computed.sqrt(precision);
    }

    protected CartesianPoint vectorOf(final CartesianPoint point1, final CartesianPoint point2) {
        return new CartesianPoint(point2.x().subtract(point1.x()), point2.y().subtract(point1.y()));
    }

}
