CREATE TRIGGER update_russian_date UPDATE OF word ON russian
  BEGIN
      UPDATE russian SET updated = DATETIME('now', 'localtime');
  END;
---
CREATE TRIGGER update_english_date UPDATE OF word ON english
  BEGIN
      UPDATE english SET updated = DATETIME('now', 'localtime');
  END;
---