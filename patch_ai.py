with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'r', encoding='utf-8') as f:
    lines = f.readlines()
    in_dialog = False
    for i, line in enumerate(lines):
        if '<el-dialog' in line and ('添加宠物' in line or 'showPetDialog' in line or 'isPetDialogOpen' in line):
            in_dialog = True
        if in_dialog:
            print(f'{i+1}: {line.strip()}')
            if '</el-dialog>' in line:
                break
