# flex-proxy

En proxy for å eksponere gitte endepunkter på en ingress og proxyer kallene bakover til en backend. Kan ses på som en edge service i arkitekturen.
Styrer også CORS som frontendene forholder seg til, slik at backend slipper det.
Flytter også over jwt fra cookie til auth header.


## yaml config

Tillatte metoder styres av routes.yaml fila. Denne bruker

## nødvendige miljøvariabler

I skrivende stund er tre miljøvariabler nødvendige.

| Variabel | Beskrivelse |
| - | - |
| `ALLOWED_ORIGINS` | Kommaseparert tillatte origins |
| `SERVICE_GATEWAY_URL` | URL til backend for applikasjonen |
| `SERVICE_GATEWAY_KEY` | Nøkkel til api gateway for applikasjonen dersom backend nås via apigw |
| `SERVICE_LIVENESS_PATH` | Endepunkt i backend proxyen kan teste forbindelse for readiness mot backend.  |

