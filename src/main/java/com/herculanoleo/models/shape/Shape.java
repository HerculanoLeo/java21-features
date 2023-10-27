package com.herculanoleo.models.shape;

import java.util.List;

public sealed interface Shape permits CircleShape, RectangleShape, TriangleShape {
    List<CartesianPoint> cartesianPoints();
}
