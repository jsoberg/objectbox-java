/*
 * Copyright (C) 2011-2016 Markus Junginger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.objectbox.query;

import java.util.Date;

import io.objectbox.Property;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;

/**
 * Internal interface to model WHERE conditions used in queries. Use the {@link Property} objects in the DAO classes to
 * create new conditions.
 */
@Experimental
@Internal
public interface QueryCondition {

    void applyTo(QueryBuilder queryBuilder);

    abstract class AbstractCondition implements QueryCondition {

        public final Object value;
        protected final Object[] values;

        public AbstractCondition(Object value) {
            this.value = value;
            this.values = null;
        }

        public AbstractCondition(Object[] values) {
            this.value = null;
            this.values = values;
        }

    }

    class PropertyCondition extends AbstractCondition {

        public enum Operation {
            EQUALS,
            NOT_EQUALS,
            BETWEEN,
            IN,
            GREATER_THAN,
            LESS_THAN,
            IS_NULL,
            IS_NOT_NULL
        }

        public final Property property;
        private final Operation operation;

        public PropertyCondition(Property property, Operation operation, Object value) {
            super(checkValueForType(property, value));
            this.property = property;
            this.operation = operation;
        }

        public PropertyCondition(Property property, Operation operation, Object[] values) {
            super(checkValuesForType(property, operation, values));
            this.property = property;
            this.operation = operation;
        }

        public void applyTo(QueryBuilder queryBuilder) {
            if (operation == Operation.EQUALS) {
                if (value instanceof Long) {
                    queryBuilder.equal(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.equal(property, (Integer) value);
                } else if (value instanceof String) {
                    queryBuilder.equal(property, (String) value);
                }
            } else if (operation == Operation.NOT_EQUALS) {
                if (value instanceof Long) {
                    queryBuilder.notEqual(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.notEqual(property, (Integer) value);
                } else if (value instanceof String) {
                    queryBuilder.notEqual(property, (String) value);
                }
            } else if (operation == Operation.BETWEEN) {
                if (values[0] instanceof Long && values[1] instanceof Long) {
                    queryBuilder.between(property, (Long) values[0], (Long) values[1]);
                } else if (values[0] instanceof Integer && values[1] instanceof Integer) {
                    queryBuilder.between(property, (Integer) values[0], (Integer) values[1]);
                }
            } else if (operation == Operation.IN) {
                // just check the first value and assume all others are of the same type
                // maybe this is too naive and we should properly check values earlier
                if (values[0] instanceof Long) {
                    long[] inValues = new long[values.length];
                    for (int i = 0; i < values.length; i++) {
                        inValues[i] = (long) values[i];
                    }
                    queryBuilder.in(property, inValues);
                } else if (values[0] instanceof Integer) {
                    int[] inValues = new int[values.length];
                    for (int i = 0; i < values.length; i++) {
                        inValues[i] = (int) values[i];
                    }
                    queryBuilder.in(property, inValues);
                }
            } else if (operation == Operation.GREATER_THAN) {
                if (value instanceof Long) {
                    queryBuilder.greater(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.greater(property, (Integer) value);
                } else if (value instanceof Double) {
                    queryBuilder.greater(property, (Double) value);
                } else if (value instanceof Float) {
                    queryBuilder.greater(property, (Float) value);
                }
            } else if (operation == Operation.LESS_THAN) {
                if (value instanceof Long) {
                    queryBuilder.less(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.less(property, (Integer) value);
                } else if (value instanceof Double) {
                    queryBuilder.less(property, (Double) value);
                } else if (value instanceof Float) {
                    queryBuilder.less(property, (Float) value);
                }
            } else if (operation == Operation.IS_NULL) {
                queryBuilder.isNull(property);
            } else if (operation == Operation.IS_NOT_NULL) {
                queryBuilder.notNull(property);
            } else {
                throw new UnsupportedOperationException("This operation is not known.");
            }
        }

        private static Object checkValueForType(Property property, Object value) {
            if (value != null && value.getClass().isArray()) {
                throw new DbException("Illegal value: found array, but simple object required");
            }
            Class<?> type = property.type;
            if (type == Date.class) {
                if (value instanceof Date) {
                    return ((Date) value).getTime();
                } else if (value instanceof Long) {
                    return value;
                } else {
                    throw new DbException("Illegal date value: expected java.util.Date or Long for value " + value);
                }
            } else if (property.type == boolean.class || property.type == Boolean.class) {
                if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1 : 0;
                } else if (value instanceof Number) {
                    int intValue = ((Number) value).intValue();
                    if (intValue != 0 && intValue != 1) {
                        throw new DbException("Illegal boolean value: numbers must be 0 or 1, but was " + value);
                    }
                } else if (value instanceof String) {
                    String stringValue = ((String) value);
                    if ("TRUE".equalsIgnoreCase(stringValue)) {
                        return 1;
                    } else if ("FALSE".equalsIgnoreCase(stringValue)) {
                        return 0;
                    } else {
                        throw new DbException(
                                "Illegal boolean value: Strings must be \"TRUE\" or \"FALSE\" (case insensitive), but was "
                                        + value);
                    }
                }
            }
            return value;
        }

        private static Object[] checkValuesForType(Property property, Operation operation, Object[] values) {
            if (values == null) {
                if (operation == Operation.IS_NULL || operation == Operation.IS_NOT_NULL) {
                    return null;
                } else {
                    throw new IllegalArgumentException("This operation requires non-null values.");
                }
            }
            for (int i = 0; i < values.length; i++) {
                values[i] = checkValueForType(property, values[i]);
            }
            return values;
        }

    }

}
