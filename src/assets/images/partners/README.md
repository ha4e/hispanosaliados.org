# Partner trade school logos

Place each school’s logo image here so it appears on the Programs page partner cards. The graphic area also links to the school’s website.

**Current assets (fetched):**  
- **1-tws.svg** – Tulsa Welding School  
- **2-uti.svg** – Universal Technical Institute  
- **3-fortis.svg** – Fortis Institute  
- **4-uei.png** – UEI College  
- **5-lonestar.svg** – Lone Star College  
- **7-houston-trade.png** – Houston Trade Training LLC  

**Placeholders only (no logo file yet):** Schools 6 (Texas Technical Trade School), 8 (ICT), 9 (Aviation Institute of Maintenance), 10 (Houston School of Carpentry). Cards 6 and 10 have no website link; 8 and 9 logos could not be fetched—add manually if the schools provide assets.

**Naming:** Use the filenames above or add new ones (e.g. `8-ict.png`, `9-aim.png`). Supported formats: PNG, JPG, SVG.

**Usage:** In `src/templates/programs.html`, inside the corresponding `.trade-school-logo-link`, add an `<img class="trade-school-logo" src="..." alt="...">` before the placeholder span. When the image is present, the numbered placeholder is hidden automatically. Keep the placeholder so the card still shows something if the image fails to load.

**Note:** Partner logos are typically trademarked. Use only with permission (e.g. as part of a formal partnership or with school approval).
