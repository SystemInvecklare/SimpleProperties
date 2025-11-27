package com.github.systeminvecklare.libs.simpleproperties.validator;

/**
 * Defines a contract for validating property values.
 * <p>
 * Implementations should check the given input and return an error message
 * if the input is invalid. If the input is valid, {@code null} should be returned.
 * </p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * IPropertyValidator validator = input -> {
 *     if (input == null || input.isEmpty()) return "Input cannot be empty";
 *     return null;
 * };
 * }</pre>
 */
public interface IPropertyValidator {

    /**
     * Validates the given input.
     *
     * @param input the property value to validate
     * @return {@code null} if the input is valid; otherwise an error message describing
     *         why the input is invalid
     */
    String validate(String input);
}
