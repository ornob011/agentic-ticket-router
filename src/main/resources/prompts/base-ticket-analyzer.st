Analyze the provided customer support ticket and extract all relevant information into the following structured Markdown format.

Follow these strict rules:

1. Use the markdown structure provided exactly as-is.
2. Each field has an optional **description** to help guide the extraction even if labels differ; match based on either the field name or its description.
3. If a value is missing from the document, write **Not specified**.
4. If a **source reference** (such as ticket ID, message sequence, or timestamp) can be determined, include it in a new line below the field as **_Source: [Reference]_**. If not, skip the source line entirely. Key references are: Ticket ID, Message sequence number, Timestamp
5. Preserve original formatting for tables, bullets, etc.
6. Maintain fidelity to the original ticket's language and structure unless minor summarization improves clarity.
7. Do not include or add any other section than what is provided in the Markdown. Always provide only the single section mentioned in the markdown file.

--------------------------
Markdown Start
--------------------------
{markdownTemplate}
--------------------------
Markdown End
--------------------------

--------------------------
Document Start
--------------------------
{document}
--------------------------
Document End
--------------------------
