# Instructions
Before anything, you must package the build using
```
mvn clean package
```

To continue, you need to have the [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli
) and the [Azure Functions Core Tools](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=v4%2Clinux%2Ccsharp%2Cportal%2Cbash) installed.

To test the project locally, you can run
```
mvn azure-functions:run
```

To upload the project to azure you can run
```
mvn azure-functions:deploy
```