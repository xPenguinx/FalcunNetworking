package me.minidigger.falcunnetworking.common.properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyTest {

    private static final Property<String> testString = new Property<String>("test.string", String.class, "dum");
    private static final Property<Integer> testInteger = new Property<Integer>("test.integer", Integer.class, 1);
    private static final Property<Integer> testIntegerError = new Property<Integer>("test.integer.error", Integer.class, 1);
    private static final Property<Double> testDouble = new Property<Double>("test.double", Double.class, 1.0);
    private static final Property<PropertyTest> testUnsupported = new Property<PropertyTest>("test.unsupported", PropertyTest.class, null);
    private static final Property<String> testUnknown = new Property<String>("test.cool", String.class, "dum");

    @BeforeAll
    public static void setup() {
        Property.load("src/test/resources/falcunnetworking.properties");
    }

    @Test
    public void test() {
        assertEquals("Hey", testString.get());
        assertEquals(1337, testInteger.get());
        assertThrows(Property.PropertyException.class, new Executable() {
            @Override
            public void execute() {
                testIntegerError.get();
            }
        });
        assertEquals(42.69, testDouble.get());
        assertThrows(Property.PropertyException.class, new Executable() {
            @Override
            public void execute() {
                testUnsupported.get();
            }
        });
        assertEquals(testUnknown.getDefaultValue(), testUnknown.get());
    }
}
