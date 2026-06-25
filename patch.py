import codecs
import re

path = 'e:/code/AndroidStudio/SHTools/app/src/main/java/com/quayquay/shtools/StartAuto.java'
with codecs.open(path, 'r', 'utf-8') as f:
    content = f.read()

pattern = r'int bottomOfTextY = labelNode\.y;.*?return null;'

replacement = '''
        java.util.List<HSQTools.TextBlock> sortedBlocks = new java.util.ArrayList<>(screen);
        java.util.Collections.sort(sortedBlocks, new java.util.Comparator<HSQTools.TextBlock>() {
            @Override
            public int compare(HSQTools.TextBlock t1, HSQTools.TextBlock t2) {
                return Integer.compare(t1.y, t2.y);
            }
        });

        java.util.List<HSQTools.TextBlock> blocksBelow = new java.util.ArrayList<>();
        for (HSQTools.TextBlock node : sortedBlocks) {
            if (node.y >= labelNode.y) {
                blocksBelow.add(node);
            }
        }

        int targetY = -1;
        for (int i = 0; i < blocksBelow.size() - 1; i++) {
            HSQTools.TextBlock current = blocksBelow.get(i);
            HSQTools.TextBlock next = blocksBelow.get(i + 1);
            int gap = next.y - current.y;
            if (gap > 130 && (current.y - labelNode.y) < 600) {
                targetY = current.y + 120;
                break;
            }
        }

        if (targetY == -1) {
            int bottomOfTextY = labelNode.y;
            for (HSQTools.TextBlock node : blocksBelow) {
                if (node.y > bottomOfTextY && node.y < bottomOfTextY + 130) {
                    bottomOfTextY = node.y;
                } else if (node.y >= bottomOfTextY + 130) {
                    break;
                }
            }
            targetY = bottomOfTextY + 120;
        }

        int blindClickX = labelNode.x + 100;
        int blindClickY = targetY; 

        if (blindClickY > 180 && blindClickY < heightOfScreen - 100) {
            updateNotificationContent("WebView Blind Fallback: Bam mu duoi mo neo Y=" + blindClickY);
            return new android.graphics.Point(blindClickX, blindClickY);
        }

        return null;
'''

content = re.sub(pattern, replacement.strip(), content, flags=re.DOTALL)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(content)
