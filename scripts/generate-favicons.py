#!/usr/bin/env python3
"""
Generate favicons from the HA4E logo.
Options:
1. Circle logo only (cropped from left side)
2. HA4E text only
"""

from PIL import Image, ImageDraw, ImageFont
import os
import sys

def generate_circle_logo_favicons():
    """Generate favicons using just the circle logo portion"""
    logo_path = 'src/assets/images/logo/FINAL-logo-HA4E-empowering-futures.png'
    output_dir = 'src/assets/images'
    
    if not os.path.exists(logo_path):
        print(f"Error: Logo file not found at {logo_path}")
        return False
    
    # Open the logo
    logo = Image.open(logo_path)
    width, height = logo.size
    
    # Assuming the circle is on the left side, crop a square from the left
    # For a 1024x1024 logo, the circle is likely in the first 512x512 or similar
    # We'll crop a square from the left-center area
    crop_size = min(width, height)
    # Crop from left-center (adjust these values if needed)
    left = 0
    top = (height - crop_size) // 2
    right = crop_size
    bottom = top + crop_size
    
    circle_logo = logo.crop((left, top, right, bottom))
    
    # Generate favicons
    sizes = {
        'favicon.png': 64,
        'favicon-32x32.png': 32,
        'favicon-16x16.png': 16
    }
    
    for filename, size in sizes.items():
        favicon = circle_logo.resize((size, size), Image.Resampling.LANCZOS)
        output_path = os.path.join(output_dir, filename)
        favicon.save(output_path)
        print(f"Generated: {output_path} ({size}x{size})")
    
    return True

def generate_text_favicons():
    """Generate favicons using just 'HA4E' text"""
    output_dir = 'src/assets/images'
    
    # Create a new image with transparent background
    sizes = {
        'favicon.png': 64,
        'favicon-32x32.png': 32,
        'favicon-16x16.png': 16
    }
    
    # Colors from your brand (adjust if needed)
    bg_color = (255, 255, 255, 0)  # Transparent
    text_color = (200, 16, 46)  # Red from HA4E brand
    
    for filename, size in sizes.items():
        # Create image with transparent background
        img = Image.new('RGBA', (size, size), bg_color)
        draw = ImageDraw.Draw(img)
        
        # Try to use a system font, fallback to default
        try:
            # Try different font paths
            font_size = int(size * 0.7)
            font_paths = [
                '/System/Library/Fonts/Helvetica.ttc',
                '/System/Library/Fonts/Arial.ttf',
                '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
            ]
            font = None
            for font_path in font_paths:
                if os.path.exists(font_path):
                    try:
                        font = ImageFont.truetype(font_path, font_size)
                        break
                    except:
                        continue
            
            if font is None:
                font = ImageFont.load_default()
        except:
            font = ImageFont.load_default()
        
        # Draw text centered
        text = "HA4E"
        bbox = draw.textbbox((0, 0), text, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
        
        x = (size - text_width) // 2
        y = (size - text_height) // 2
        
        draw.text((x, y), text, fill=text_color, font=font)
        
        output_path = os.path.join(output_dir, filename)
        img.save(output_path)
        print(f"Generated: {output_path} ({size}x{size})")
    
    return True

if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == '--text':
        print("Generating text-only favicons (HA4E)...")
        success = generate_text_favicons()
    else:
        print("Generating circle logo favicons...")
        print("(Use --text flag for text-only version)")
        success = generate_circle_logo_favicons()
    
    if success:
        print("\n✓ Favicons generated successfully!")
        print("Run 'make build' to include them in the build.")
    else:
        print("\n✗ Failed to generate favicons")
        sys.exit(1)
