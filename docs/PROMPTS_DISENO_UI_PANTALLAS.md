# PROMPTS DE DISEÑO UI — MultiPOS (Mockups para generador de imágenes)

> Prompts listos para pegar en un generador de imágenes (Midjourney, DALL·E, etc.).
> Pegar el **bloque de estilo común** + la pantalla deseada para mantener coherencia entre los 3 mockups.

---

## Bloque de estilo común (usar en los 3)

```
Flat 3D UI mockup for a premium Android point-of-sale app named "MultiPOS". Clean material design, generous white space, 16px grid rhythm. Style reference: modern fintech apps (Stripe Cash, Revolut, Square). Soft shadows, 16–24px corner radius, emerald/teal accent #00BFA5, background warm grey #F5F7FA, white cards, subtle border #E0E6ED. Crisp minimalist icons, large bold numbers, Spanish UI text. No camera UI chrome, no bezels. Vertical phone frame, edge-to-edge screen.
```

---

## Prompt 1 — Login / Splash

```
A mobile phone mockup of the login screen for a premium POS app called "MultiPOS", upright vertical orientation, edge-to-edge screen, no bezel.
Top third: a circular rounded logo badge (abstract rising-bird or layered-parallelogram emblem) colored in emerald teal #00BFA5 on white, with a soft teal radial-glow behind it.
Below the logo: the brand name "MultiPOS" in a heavy condensed Sans-Serif, black weight, black ink #1A1C1E, letter-spaced, followed by the small tagline "Caja • Ventas • Créditos" in muted blue-grey #64748B.
Centre: two rounded-pill input fields, white with subtle #E0E6ED border, inside them placeholder light-grey text "Usuario" and "Contraseña" with a small lock icon on the right; the second field has a visibility eye icon.
Below: one full-width emerald #00BFA5 button with white bold text "ENTRAR", slight bottom shadow, rounded 16px corners.
Under it a grey link "¿Olvidaste tu contraseña?".
Bottom of screen: tiny muted footer text "MultiPOS · Punto de Venta v2.0".
Background is light warm-grey #F5F7FA; generous spacing, minimal, executive, no clutter, no gradients on background, only on the brand badge. Photorealistic phone mockup, studio light.
```

---

## Prompt 2 — Dashboard principal

```
A mobile phone mockup of the main dashboard screen of a premium POS app "MultiPOS", upright vertical orientation, edge-to-edge screen, no bezel.
Top app bar: small brand mark + title "Dashboard" in heavy condensed Sans-Serif black #1A1C1E, below it the subtitle "Resumen de tu negocio" in muted grey #64748B. Top-right corner: a small emerald pill toggle "Hoy ▾".
Hero card (full width): white card, rounded 24px, soft shadow. Left: 48px squircle icon container filled emerald-soft #E0F2F1 with a shopping-cart icon in emerald #00BFA5. Label "Ventas Hoy" in small bold grey, then the number "Bs 12,505" in 40px Black weight emerald #00BFA5. Under it a tiny green chip "▲ 18%" with caption "vs ayer".
Second row: two equal white cards side by side, each with a small outlined icon, a label in grey ("Productos" and "Stock Bajo") and a big number in Black weight (45 in ink, 3 in red #E53935 with a small warning triangle).
Bottom area: a white card with a thin horizontal stacked bar chart (segments emerald, teal, orange) and legend "Efectivo · Tarjeta · Crédito".
Background light warm-grey #F5F7FA. Executive, clean, data-focused, high contrast numbers, soft shadows, no bezel, no device frame chrome. Photorealistic phone mockup, studio light.
```

---

## Prompt 3 — Pantalla de Punto de Venta (caja)

```
A mobile phone mockup of the sales screen of a premium POS app "MultiPOS", upright vertical orientation, edge-to-edge screen, no bezel.
Top app bar filled solid emerald #00BFA5: left white bold title "Punto de Venta", right a white QR-scanner icon in a white rounded square chip.
Below: a full-width rounded search field, white, subtle border, placeholder "Buscar producto por nombre o código…" with a magnifier icon.
Main body split in two panels side by side:
LEFT panel ("catálogo", ~60% width): a vertical list of white product cards; each card shows product name in medium black Sans-Serif, small grey code line "Cód: PROD001", right-aligned price in emerald bold, and a tiny stock pill (green "Stock ok" for most, amber "Stock: 3" for one, red "Stock: 0" for another). Cards rounded 16px, 1dp shadow, stacked with 12px gaps.
RIGHT panel ("carrito", ~40% width): a light grey #F1F5F9 container with title "Carrito" and small red link "Limpiar". Inside: two white cart-line rows, each with product name, a stepper [– 2 +] (round outlined icon buttons, quantity in bold centre), and subtotal in emerald bold. A big empty-state illustration area only if empty.
Bottom of the right panel, a white summary card: rows "Cliente: No aplica" with a small QR icon, "Método de pago:" with an emerald chip "EFECTIVO", a divider, then "Total:" (grey) on the left and "Bs 52.00" in 26px Black emerald on the right, and below a full-width emerald button "COBRAR" with white bold uppercase text and soft shadow.
Background light warm-grey. Photorealistic phone mockup, studio light, crisp typography, no bezel chrome.
```

---

## Consejos de uso

- Mantener el **bloque de estilo común** en los 3 para que parezcan la misma app.
- Ratio vertical: `--ar 9:19` en Midjourney, o "vertical 9:16" en otros generadores.
- Si la herramienta permite regenerar regiones, generar cada pantalla y pedir "apply the same style to the next screen".
