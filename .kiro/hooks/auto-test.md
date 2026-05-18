# Auto Test Generator Hook

## Trigger
When any Java file in backend/src/main is saved

## Action
Automatically generate or update the corresponding 
unit test file in backend/src/test folder.

## Rules
- Use JUnit 5 for tests
- Test all public methods
- Add mock data using Mockito
- Follow naming convention: ClassNameTest.java