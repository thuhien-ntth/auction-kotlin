import os

file_path = r'C:\Users\thuhi\Project\mobile\fe\app\src\main\kotlin\com\auction\app\ui\screens\detail\ProductDetailScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('Ch?nh s?a thng tin', 'Chỉnh sửa thông tin')
content = content.replace('s?n ph?m dang ch? duy?t -> nt Ch?nh s?a', 'sản phẩm đang chờ duyệt -> nút Chỉnh sửa')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Done')
