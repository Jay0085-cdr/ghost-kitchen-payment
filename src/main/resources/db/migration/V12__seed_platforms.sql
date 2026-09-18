-- Reference data only (channel names) — no deduction rules or column mappings,
-- per the "no invented platform rules" principle in ARCHITECTURE.md Section 2.
INSERT INTO platform (code, display_name) VALUES
    ('SWIGGY', 'Swiggy'),
    ('ZOMATO', 'Zomato'),
    ('DIRECT', 'Direct / WhatsApp Orders');
