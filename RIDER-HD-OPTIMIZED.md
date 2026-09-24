# CobblemonRider HD Optimized V5

Safe optimization over V4.

- Preserves V4/V2 lookup semantics and right-click mount behavior.
- Adds a per-Pokemon resolved-config cache keyed by config identity + species + form.
- Caches null results too, avoiding repeated scans for unsupported forms.
- Automatically invalidates when species/form changes or a new config object is received.
- Resets max-passenger cache after a form/config transition.
- Keeps GZIP large-config sync and adds server-side encoded-payload reuse for multiple joins.
- Adds passenger-offset bounds checks to avoid malformed offset lists crashing rider positioning.
- Same network packet format/protocol as V4.
- Same modId: cobblemonrider.
