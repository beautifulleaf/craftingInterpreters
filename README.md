# craftingInterpreters
My implementation of Crafting Interpreters. Work in progress.

To-do list by chapter of book
Scanning: 
- Adding support for C-style block commments (allowing nesting). 
- Add an "ErrorReporter" interface and have more specific error reporting. 
- Find a way to coalesce a run of invalid characters into a single error.
- Support escape sequences. 
- Support decimals that don't start with a 0 (e.g. .1234).
- Allow methods on numbers (123.sqrt).
- Remove need for semicolons. 
