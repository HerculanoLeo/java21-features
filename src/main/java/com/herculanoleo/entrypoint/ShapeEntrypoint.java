package com.herculanoleo.entrypoint;

import com.herculanoleo.models.exception.InvalidShapeException;
import com.herculanoleo.models.exception.JumpingOptionsException;
import com.herculanoleo.models.shape.*;
import com.herculanoleo.processor.ProcessorShape;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Scanner;

@Log4j2
public class ShapeEntrypoint implements Entrypoint {

    public final static Entrypoint shared = new ShapeEntrypoint();

    protected final static Scanner scanner = new Scanner(System.in);

    protected final static List<String> alphabetic = List.of("A", "B", "C", "D");

    @Override
    public void start() {
        var selectedOption = -1;

        while (selectedOption != 0) {
            System.out.println("_".repeat(20) + "Shape" + "_".repeat(20));
            System.out.println("1 - Type of Shape");
            System.out.println("2 - Distances");
            System.out.println("3 - Area");
            System.out.println("4 - Angle");
            System.out.println("0 - Exit");

            try {
                selectedOption = scanner.nextInt();

                switch (selectedOption) {
                    case 1 -> typeOf();
                    case 2 -> distancesOf();
                    case 3 -> areaOf();
                    case 4 -> anglesOf();
                    case 0 -> System.out.println("Exit Shape");
                    default -> throw new RuntimeException("Invalid option");
                }
            } catch (JumpingOptionsException ignored) {
            } catch (Exception ex) {
                System.out.println("Please enter with a valid option");
                log.error("An error occurred", ex);
            }
        }
    }

    protected void typeOf() {
        try {
            var shape = createShape();
            var type = ProcessorShape.shared.typeOf(shape);
            System.out.printf("Result of typeOf is %s%n", type.name());
        } catch (InvalidShapeException ex) {
            System.out.println("Invalid points, cannot determinate the type of shape");
        }
    }

    protected void distancesOf() {
        try {
            var shape = createShape();
            var distances = ProcessorShape.shared.distancesOf(shape);

            for (var i = 0; i < distances.size(); i++) {
                if (i == (distances.size() - 1)) {
                    System.out.printf(
                            "The distance between point %s to point %s are %s%n",
                            alphabetic.get(i),
                            alphabetic.get(0),
                            distances.get(i)
                    );
                } else {
                    System.out.printf(
                            "The distance between point %s to point %s are %s%n",
                            alphabetic.get(i),
                            alphabetic.get(i + 1),
                            distances.get(i)
                    );
                }
            }
        } catch (InvalidShapeException ex) {
            System.out.println("Invalid points, cannot determinate the vector distances of shape");
        }
    }

    protected void areaOf() {
        try {
            var shape = createShape();
            var area = ProcessorShape.shared.areaOf(shape);
            System.out.printf("The area of shape is %s%n", area);
        } catch (InvalidShapeException ex) {
            System.out.println("Invalid points, cannot determinate the area of shape");
        }
    }

    protected void anglesOf() {
        try {
            var shape = createShape();
            var angles = ProcessorShape.shared.anglesOf(shape);

            for (var i = 0; i < angles.size(); i++) {
                if (i == 0) {
                    System.out.printf(
                            "The angle of %s%s%s is %s%n",
                            alphabetic.get(angles.size() - 1),
                            alphabetic.get(i),
                            alphabetic.get(i + 1),
                            angles.get(i)
                    );
                } else if (i < (angles.size() - 1)) {
                    System.out.printf(
                            "The angle of %s%s%s is %s%n",
                            alphabetic.get(i - 1),
                            alphabetic.get(i),
                            alphabetic.get(i + 1),
                            angles.get(i)
                    );
                } else {
                    System.out.printf(
                            "The angle of %s%s%s is %s%n",
                            alphabetic.get(i - 1),
                            alphabetic.get(i),
                            alphabetic.get(0),
                            angles.get(i)
                    );
                }
            }
        } catch (InvalidShapeException ex) {
            System.out.println("Invalid points, cannot determinate the angles of shape");
        }
    }

    protected Shape createShape() {
        System.out.println("What shape do you want?");
        System.out.println("1 - Circle");
        System.out.println("2 - Rect");
        System.out.println("3 - Triangle");
        System.out.println("0 - Exit");

        while (true) {
            try {
                var selectedOption = scanner.nextInt();

                System.out.println("_".repeat(10));
                return switch (selectedOption) {
                    case 1 -> createCircle();
                    case 2 -> createRect();
                    case 3 -> createTriangle();
                    case 0 -> throw new JumpingOptionsException();
                    default -> throw new RuntimeException("Invalid option");
                };

            } catch (JumpingOptionsException e) {
                throw e;
            } catch (Exception ex) {
                System.out.println("Please enter with a valid option");
                log.error("An error occurred", ex);
            }
        }
    }

    protected RectangleShape createRect() {
        System.out.println("A rect shape has 4 cartesian points");

        System.out.println("Point A");
        var a = captureCartesianPoint();

        System.out.println("Point B");
        var b = captureCartesianPoint();

        System.out.println("Point C");
        var c = captureCartesianPoint();

        System.out.println("Point D");
        var d = captureCartesianPoint();

        return new RectangleShape(a, b, c, d);
    }

    protected TriangleShape createTriangle() {
        System.out.println("A rect shape has 3 cartesian points");

        System.out.println("Point A");
        var a = captureCartesianPoint();

        System.out.println("Point B");
        var b = captureCartesianPoint();

        System.out.println("Point C");
        var c = captureCartesianPoint();

        return new TriangleShape(a, b, c);
    }

    protected CircleShape createCircle() {
        System.out.println("A rect shape has 2 cartesian points:");

        System.out.println("Point A");
        var a = captureCartesianPoint();

        System.out.println("Point B");
        var b = captureCartesianPoint();

        return new CircleShape(a, b);
    }

    protected CartesianPoint captureCartesianPoint() {
        try {
            System.out.println("_".repeat(10));
            System.out.println("Provide the cartesian points");

            System.out.print("X Axis: ");
            var x = scanner.nextBigDecimal();

            System.out.print("Y Axis: ");
            var y = scanner.nextBigDecimal();

            return new CartesianPoint(x, y);
        } catch (Exception e) {
            System.out.println("Please provide a valid cartesian points");
            return captureCartesianPoint();
        }
    }
}
