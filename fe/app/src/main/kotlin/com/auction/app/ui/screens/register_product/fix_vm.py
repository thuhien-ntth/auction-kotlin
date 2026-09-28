import os

file_path = r'C:\Users\thuhi\Project\mobile\fe\app\src\main\kotlin\com\auction\app\ui\screens\register_product\ProductEditViewModel.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('L?i t?i s?n ph?m', 'Lỗi tải sản phẩm')
content = content.replace('C?p nh?t s?n ph?m thnh cng!', 'Cập nhật sản phẩm thành công!')
content = content.replace('C?p nh?t thng tin thnh cng nhung l?i c?p nh?t ?nh', 'Cập nhật thông tin thành công nhưng lỗi cập nhật ảnh')
content = content.replace('L?i khng xc d?nh khi c?p nh?t ?nh', 'Lỗi không xác định khi cập nhật ảnh')
content = content.replace('L?i khng xc d?nh', 'Lỗi không xác định')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Done')
