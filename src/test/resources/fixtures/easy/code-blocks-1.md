Hilla integrates a Spring Boot Java backend with a reactive TypeScript front end. It helps you build apps faster with type-safe server communication, included UI components, and integrated tooling.

## Simple type-safe server communication

Hilla helps you access the backend easily with type-safe endpoints.

`index.ts`

```ts
// Type info is automatically generated based on Java
import Person from 'Frontend/generated/com/vaadin/hilla/demo/entity/Person';
import { PersonService } from 'Frontend/generated/endpoints';

async function getPeopleWithPhoneNumber() {
  const people: Person[] = await PersonService.findAll();

  // Compile error: The property is 'phone', not 'phoneNumber'
  return people.filter((person) => !!person.phoneNumber);
}

console.log('People with phone numbers: ', getPeopleWithPhoneNumber());
```

`PersonService.java`

```java
@BrowserCallable
@AnonymousAllowed
public class PersonService {

    private PersonRepository repository;

    public PersonService(PersonRepository repository) {
        this.repository = repository;
    }

    public @Nonnull List<@Nonnull Person> findAll() {
        return repository.findAll();
    }
}
```
