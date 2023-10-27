package com.herculanoleo.processor;

import com.herculanoleo.models.exception.InvalidShapeException;
import com.herculanoleo.models.shape.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ProcessorShapeTest {

    @Spy
    private ProcessorShape processorShape;

    @Test
    public void typeOfCircleTest() {
        var shape = new CircleShape(
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.0))
        );

        var expectedResult = ShapeType.CIRCLE;

        var result = processorShape.typeOf(shape);

        assertEquals(expectedResult, result);
    }

    @Test
    public void typeOfCircleInvalidTest() {
        var shape = new CircleShape(new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)), null);
        assertThrows(InvalidShapeException.class, () -> processorShape.circleTypeOf(shape));
    }

    @Test
    public void typeOfSquareTest() {
        var shape = new RectangleShape(
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.0)),
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(4.0))
        );

        var expectedValue = ShapeType.SQUARE;

        var result = processorShape.typeOf(shape);

        assertEquals(expectedValue, result);
    }

    @Test
    public void typeOfRectangleTest() {
        var shape = new RectangleShape(
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(2.0)),
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(2.0))
        );

        var expectedResult = ShapeType.RECTANGLE;

        var result = processorShape.typeOf(shape);

        assertEquals(expectedResult, result);
    }

    @Test
    public void typeOfRectInvalidTest() {
        var shape = new RectangleShape(
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(2.0)),
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(1.0))
        );

        assertThrows(InvalidShapeException.class, () -> processorShape.rectTypeOf(shape));
    }

    @Test
    public void typeOfEquilateralTriangleTest() {
        var shape = new TriangleShape(
                new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(10), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(8.6601))
        );

        var expectedValue = ShapeType.EQUILATERAL_TRIANGLE;

        var result = processorShape.typeOf(shape);

        assertEquals(expectedValue, result);
    }

    @Test
    public void typeOfIsoscelesTriangleTest() {
        var shape = new TriangleShape(
                new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(10), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(9))
        );

        var expectedValue = ShapeType.ISOSCELES_TRIANGLE;

        var result = processorShape.typeOf(shape);

        assertEquals(expectedValue, result);
    }

    @Test
    public void typeOfScaleneTriangleTest() {
        var shape = new TriangleShape(
                new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(8), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(9))
        );

        var expectedValue = ShapeType.SCALENE_TRIANGLE;

        var result = processorShape.typeOf(shape);

        assertEquals(expectedValue, result);
    }

    @Test
    public void typeOfTriangleInvalidTest() {
        var shape = new TriangleShape(new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)), null, null);
        assertThrows(InvalidShapeException.class, () -> processorShape.triangleTypeOf(shape));
    }

    @Test
    public void typeOfNullTest() {
        assertThrows(InvalidShapeException.class, () -> processorShape.typeOf(null));
    }

    @Test
    public void distanceOfTest() {
        var a = new CartesianPoint(BigDecimal.valueOf(1.0), BigDecimal.valueOf(3.0));
        var b = new CartesianPoint(BigDecimal.valueOf(3.0), BigDecimal.valueOf(9.0));

        var expectedResult = BigDecimal.valueOf(6.32);

        assertEquals(expectedResult, processorShape.distanceOf(a, b));
    }

    @Test
    public void areaOfCircleTest() {
        var shape = new CircleShape(
                new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0))
        );

        var expectedResult = BigDecimal.valueOf(157);

        var result = processorShape.areaOf(shape);

        assertEquals(expectedResult, result);
    }

    @Test
    public void areaOfSquareTest() {
        var shape = new RectangleShape(
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.0)),
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(4.0))
        );

        var expectedResult = BigDecimal.valueOf(16);

        var result = processorShape.areaOf(shape);

        assertEquals(0, expectedResult.compareTo(result));
    }

    @Test
    public void areaOfRectangleTest() {
        var shape = new RectangleShape(
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(0.0)),
                new CartesianPoint(BigDecimal.valueOf(4.0), BigDecimal.valueOf(2.0)),
                new CartesianPoint(BigDecimal.valueOf(0.0), BigDecimal.valueOf(2.0))
        );

        var expectedResult = BigDecimal.valueOf(8);

        var result = processorShape.areaOf(shape);

        assertEquals(0, expectedResult.compareTo(result));
    }

    @Test
    public void areaOfTriangleTest() {
        var shape = new TriangleShape(
                new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(8), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(9))
        );

        var expectedResult = BigDecimal.valueOf(36.1);

        var result = processorShape.areaOf(shape);

        assertEquals(0, expectedResult.compareTo(result));
    }

    @Test
    public void areaOfInvalidTest() {
        assertThrows(InvalidShapeException.class, () -> processorShape.areaOf(null));
    }

    @Test
    public void angleOfTest() {
        var a = new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0));
        var b = new CartesianPoint(BigDecimal.valueOf(10), BigDecimal.valueOf(0));
        var c = new CartesianPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(8.66));

        var expectedResult = BigDecimal.valueOf(60);

        assertEquals(0, expectedResult.compareTo(processorShape.angleOf(a, c, b)));
        assertEquals(0, expectedResult.compareTo(processorShape.angleOf(b, a, c)));
        assertEquals(0, expectedResult.compareTo(processorShape.angleOf(c, a, b)));
    }

    @Test
    public void anglesOfTest() {
        var shape = new TriangleShape(
                new CartesianPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(10), BigDecimal.valueOf(0)),
                new CartesianPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(8.6601))
        );

        var expectedResult = List.of(
                BigDecimal.valueOf(60.0).setScale(2, RoundingMode.HALF_EVEN),
                BigDecimal.valueOf(60.0).setScale(2, RoundingMode.HALF_EVEN),
                BigDecimal.valueOf(60.0).setScale(2, RoundingMode.HALF_EVEN)
        );

        var result = processorShape.anglesOf(shape);

        assertEquals(expectedResult, result);
    }

}
