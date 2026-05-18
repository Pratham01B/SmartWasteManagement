# Security Check Hook

## Trigger
Before every Git commit in the project

## Action
Automatically scan all files for:
- Hardcoded passwords
- API keys or secret tokens
- Database credentials
- Any sensitive information

## Rules
- If any sensitive data found — show warning
- Block commit until issue is fixed
- Suggest using environment variables instead