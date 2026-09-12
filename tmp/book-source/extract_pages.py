from pypdf import PdfReader

BOOK = r"D:\schinese\tmp\slide-design\spec-driven-agent-driven-development.pdf"
reader = PdfReader(BOOK)
with open(r"D:\schinese\tmp\book-source\book.txt", "w", encoding="utf-8") as output:
    for number, page in enumerate(reader.pages, start=1):
        output.write(f"\n\n===== PDF PAGE {number} =====\n\n")
        output.write(page.extract_text() or "")

def print_pages(label, pages, limit):
    for printed_page in pages:
        text = reader.pages[printed_page - 1].extract_text() or ""
        print(f"\n===== {label} {printed_page} =====\n{text[:limit]}")

print_pages("FRONT MATTER PDF", range(1, 19), 2400)
print_pages(
    "KEY BOOK PAGE",
    [108, 110, 114, 115, 121, 168, 172, 176, 185, 207, 214, 318, 320, 323, 336],
    3200,
)
