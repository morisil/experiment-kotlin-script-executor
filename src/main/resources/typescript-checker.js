// Simple TypeScript type checker wrapper
// This demonstrates basic type checking for common TypeScript patterns

function typeCheckScript(code) {
    const errors = [];

    // Check for basic type annotations
    const typeAnnotationRegex = /:\s*(\w+)/g;
    const matches = [...code.matchAll(typeAnnotationRegex)];

    // Check for type mismatches in assignments
    const numberAssignRegex = /(\w+):\s*number\s*=\s*["'`]/g;
    const stringAssignRegex = /(\w+):\s*string\s*=\s*\d+/g;
    const booleanAssignRegex = /(\w+):\s*boolean\s*=\s*(?!true|false)\w+/g;

    // Check number type mismatches
    let match;
    while ((match = numberAssignRegex.exec(code)) !== null) {
        errors.push(`Type 'string' is not assignable to type 'number' for variable '${match[1]}'`);
    }

    // Check string type mismatches
    while ((match = stringAssignRegex.exec(code)) !== null) {
        errors.push(`Type 'number' is not assignable to type 'string' for variable '${match[1]}'`);
    }

    // Check boolean type mismatches
    while ((match = booleanAssignRegex.exec(code)) !== null) {
        errors.push(`Type mismatch for boolean variable '${match[1]}'`);
    }

    return {
        success: errors.length === 0,
        errors: errors
    };
}

// Export for use in GraalVM
typeCheckScript;
