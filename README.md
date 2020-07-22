# flex-proxy

En (forhåpentligvis) enkel proxy for å knytte applikasjoner i forskjellige soner sammen.
Prinsippet er likt [syfoproxy](https://github.com/navikt/syfoproxy), men med restriksjoner
på hvilke endepunkter som er lov å forespørre. Dette gjøres gjennom en yaml-fil.

## yaml config

Tillatte metoder akkurat nå er `GET`, `POST`, `DELETE` og `PUT`, så om du har et endepunkt `/api/v1/test` som
tillater alle metodene, og et endepunkt `/api/v1/list` som kun tillater `GET` kan du skrive:


```yaml
GET:
    api:
        v1:
            - list
            - test
POST:
    - api/v1/test
DELETE:
    api:
        - v1/test
PUT:
    api:
        v1:
            test:
# /api/v1/test i POST, DELETE og PUT vil behandles på samme måte
```

## nødvendige miljøvariabler

I skrivende stund er tre miljøvariabler nødvendige.

| Variabel | Beskrivelse |
| - | - |
| `PROXY_CONFIG` | Sti til yaml config |
| `SERVICE_GATEWAY_URL` | URL til gateway for applikasjonen |
| `SERVICE_GATEWAY_KEY` | Nøkkel til gateway for applikasjonen |

Det `SERVICE_GATEWAY_URL` og `SERVICE_GATEWAY_KEY` handler om er beskrevet greit
i [dette StackOverflow-spørsmålet](https://stackoverflow.com/c/nav-it/questions/324/646#646)