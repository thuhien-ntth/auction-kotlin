import os

file_path = r'C:\Users\thuhi\Project\mobile\fe\app\src\main\kotlin\com\auction\app\ui\screens\register_product\ProductEditScreen.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('fun ProductRegisterScreen(navController: NavController)', 'fun ProductEditScreen(productId: String, navController: NavController)')
content = content.replace('ProductRegisterViewModel = viewModel(factory = ProductRegisterViewModel.factory(container.repository))', 'ProductEditViewModel = viewModel(factory = ProductEditViewModel.factory(container.repository, productId))')
content = content.replace('ProductRegisterScreenContent', 'ProductEditScreenContent')
content = content.replace('ProductRegisterUiState', 'ProductEditUiState')
content = content.replace('currentRoute = Routes.PRODUCT_REGISTER, title = "Đăng sản phẩm mới"', 'currentRoute = "", title = "Cập nhật sản phẩm"')
content = content.replace('"Gửi yêu cầu đấu giá"', '"Cập nhật sản phẩm"')
content = content.replace('ProductRegisterScreenPreview', 'ProductEditScreenPreview')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('Done')
