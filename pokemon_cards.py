import json
from pathlib import Path

import requests

API_URL = "https://api.pokemontcg.io/v2/cards"
PAGE_SIZE = 250
SELECT_FIELDS = "id,name,images,set,types,subtypes,rarity"
OUTPUT_PATH = Path("PokeBinder/app/src/main/assets/pokemon_cards.json")


def fetch_all_cards() -> list[dict]:
    cards: list[dict] = []
    page = 1
    total_count = None

    while True:
        response = requests.get(
            API_URL,
            params={
                "page": page,
                "pageSize": PAGE_SIZE,
                "select": SELECT_FIELDS,
            },
            timeout=30,
        )
        response.raise_for_status()

        payload = response.json()
        page_cards = payload.get("data", [])
        total_count = payload.get("totalCount", total_count)

        if not page_cards:
            break

        for card in page_cards:
            cards.append(
                {
                    "cardId": card.get("id", ""),
                    "name": card.get("name", ""),
                    "imageUrl": card.get("images", {}).get("small", ""),
                    "setName": card.get("set", {}).get("name", ""),
                    "types": sanitize_list(card.get("types")),
                    "subtypes": sanitize_list(card.get("subtypes")),
                    "rarity": (card.get("rarity") or "").strip(),
                }
            )

        print(
            f"Fetched page {page} with {len(page_cards)} cards "
            f"({len(cards)} total so far)."
        )

        if total_count is not None and len(cards) >= total_count:
            break

        page += 1

    cards.sort(
        key=lambda card: (
            card["name"].lower(),
            card["setName"].lower(),
            card["cardId"].lower(),
        )
    )
    return cards


def sanitize_list(values: list[str] | None) -> list[str]:
    return [
        value.strip()
        for value in (values or [])
        if isinstance(value, str) and value.strip()
    ]


def main() -> None:
    cards = fetch_all_cards()
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(json.dumps(cards, indent=2), encoding="utf-8")
    print(f"Wrote {len(cards)} cards to {OUTPUT_PATH}")


if __name__ == "__main__":
    main()