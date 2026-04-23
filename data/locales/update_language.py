import json
import os

# 定义需要补充的键值对（语言代码 -> 补充内容）
# 对于未明确提供的语言，使用简体中文作为后备（可根据需要调整）
additional_keys = {
    "zh": {  # 简体中文
        "select_download_method": "选择下载方式",
        "no_download_options": "该平台暂无可用下载链接，请联系管理员。",
        "no_valid_download_source": "没有有效的下载链接",
        "download_from_github": "GitHub 下载",
        "download_from_lanzouyun": "蓝奏云下载",
        "source_github": "GitHub 存储",
        "source_lanzouyun": "蓝奏云存储",
        "community_page_title": "社区分享",
        "suggestions_title": "功能建议与改进 – 让我们一起构建！",
        "suggestions_description": "我们非常重视每一位用户的反馈。欢迎您在这里提出功能建议、改进意见或分享您的创作。请保持文明、友善的交流。",
        "suggestions_button": "前往讨论区 →",
        "loading_community": "加载社区信息中...",
        "no_discussion_link": "未找到讨论区链接"
    },
    "zh-TW": {  # 繁體中文
        "select_download_method": "選擇下載方式",
        "no_download_options": "該平台暫無可用下載連結，請聯絡管理員。",
        "no_valid_download_source": "沒有有效的下載連結",
        "download_from_github": "GitHub 下載",
        "download_from_lanzouyun": "藍奏雲下載",
        "source_github": "GitHub 存儲",
        "source_lanzouyun": "藍奏雲存儲",
        "community_page_title": "社群分享",
        "suggestions_title": "功能建議與改進 – 讓我們一起構建！",
        "suggestions_description": "我們非常重視每一位用戶的反饋。歡迎您在這裡提出功能建議、改進意見或分享您的創作。請保持文明、友善的交流。",
        "suggestions_button": "前往討論區 →",
        "loading_community": "載入社群資訊中...",
        "no_discussion_link": "未找到討論區連結"
    },
    "en": {  # English
        "select_download_method": "Select Download Method",
        "no_download_options": "No download links available for this platform. Please contact the administrator.",
        "no_valid_download_source": "No valid download source",
        "download_from_github": "Download from GitHub",
        "download_from_lanzouyun": "Download from Lanzouyun",
        "source_github": "GitHub Repository",
        "source_lanzouyun": "Lanzouyun Storage",
        "community_page_title": "Community Share",
        "suggestions_title": "Feature Suggestions & Improvements – Let's Build Together!",
        "suggestions_description": "We value every user's feedback. Welcome to share your feature suggestions, improvement ideas, or creations here. Please keep your communication civil and friendly.",
        "suggestions_button": "Go to Discussions →",
        "loading_community": "Loading community info...",
        "no_discussion_link": "Discussion link not found"
    },
    "de": {  # Deutsch
        "select_download_method": "Downloadmethode auswählen",
        "no_download_options": "Für diese Plattform sind keine Downloadlinks verfügbar. Bitte kontaktieren Sie den Administrator.",
        "no_valid_download_source": "Keine gültige Downloadquelle",
        "download_from_github": "Von GitHub herunterladen",
        "download_from_lanzouyun": "Von Lanzouyun herunterladen",
        "source_github": "GitHub-Repository",
        "source_lanzouyun": "Lanzouyun-Speicher",
        "community_page_title": "Community",
        "suggestions_title": "Funktionsvorschläge & Verbesserungen – Lasst uns gemeinsam bauen!",
        "suggestions_description": "Wir schätzen jedes Feedback. Teilen Sie hier gerne Ihre Funktionsvorschläge, Verbesserungsideen oder Kreationen. Bitte bleiben Sie respektvoll und freundlich.",
        "suggestions_button": "Zu den Diskussionen →",
        "loading_community": "Lade Community-Informationen...",
        "no_discussion_link": "Diskussionslink nicht gefunden"
    },
    "fr": {  # Français
        "select_download_method": "Choisissez la méthode de téléchargement",
        "no_download_options": "Aucun lien de téléchargement disponible pour cette plateforme. Veuillez contacter l'administrateur.",
        "no_valid_download_source": "Aucune source de téléchargement valide",
        "download_from_github": "Télécharger depuis GitHub",
        "download_from_lanzouyun": "Télécharger depuis Lanzouyun",
        "source_github": "Dépôt GitHub",
        "source_lanzouyun": "Stockage Lanzouyun",
        "community_page_title": "Partage communautaire",
        "suggestions_title": "Suggestions de fonctionnalités & améliorations – Construisons ensemble !",
        "suggestions_description": "Nous apprécions tous les retours. Partagez ici vos suggestions, idées d'amélioration ou créations. Veuillez rester courtois et amical.",
        "suggestions_button": "Aller aux discussions →",
        "loading_community": "Chargement des infos communautaires...",
        "no_discussion_link": "Lien de discussion non trouvé"
    },
    "ja": {  # 日本語
        "select_download_method": "ダウンロード方法を選択",
        "no_download_options": "このプラットフォームには利用可能なダウンロードリンクがありません。管理者にお問い合わせください。",
        "no_valid_download_source": "有効なダウンロードソースがありません",
        "download_from_github": "GitHubからダウンロード",
        "download_from_lanzouyun": "Lanzouyunからダウンロード",
        "source_github": "GitHubリポジトリ",
        "source_lanzouyun": "Lanzouyunストレージ",
        "community_page_title": "コミュニティ共有",
        "suggestions_title": "機能提案と改善 – 一緒に作りましょう！",
        "suggestions_description": "すべてのフィードバックを大切にします。機能提案、改善アイデア、作品をここで共有してください。礼儀正しく、友好的なコミュニケーションをお願いします。",
        "suggestions_button": "ディスカッションへ →",
        "loading_community": "コミュニティ情報を読み込み中...",
        "no_discussion_link": "ディスカッションリンクが見つかりません"
    },
    "ko": {  # 한국어
        "select_download_method": "다운로드 방법 선택",
        "no_download_options": "이 플랫폼에 사용 가능한 다운로드 링크가 없습니다. 관리자에게 문의하세요.",
        "no_valid_download_source": "유효한 다운로드 소스가 없음",
        "download_from_github": "GitHub에서 다운로드",
        "download_from_lanzouyun": "Lanzouyun에서 다운로드",
        "source_github": "GitHub 저장소",
        "source_lanzouyun": "Lanzouyun 스토리지",
        "community_page_title": "커뮤니티 공유",
        "suggestions_title": "기능 제안 및 개선 – 함께 만들어요!",
        "suggestions_description": "모든 피드백을 소중히 여깁니다. 기능 제안, 개선 아이디어 또는 창작물을 여기에 공유하세요. 예의 바르고 친근한 대화를 부탁드립니다.",
        "suggestions_button": "토론하러 가기 →",
        "loading_community": "커뮤니티 정보 로딩 중...",
        "no_discussion_link": "토론 링크를 찾을 수 없음"
    },
    "pt": {  # Português
        "select_download_method": "Selecione o método de download",
        "no_download_options": "Não há links de download disponíveis para esta plataforma. Contate o administrador.",
        "no_valid_download_source": "Nenhuma fonte de download válida",
        "download_from_github": "Baixar do GitHub",
        "download_from_lanzouyun": "Baixar do Lanzouyun",
        "source_github": "Repositório GitHub",
        "source_lanzouyun": "Armazenamento Lanzouyun",
        "community_page_title": "Compartilhamento Comunitário",
        "suggestions_title": "Sugestões de Recursos & Melhorias – Vamos Construir Juntos!",
        "suggestions_description": "Valorizamos todos os feedbacks. Compartilhe aqui suas sugestões, ideias de melhoria ou criações. Por favor, mantenha uma comunicação respeitosa e amigável.",
        "suggestions_button": "Ir para Discussões →",
        "loading_community": "Carregando informações da comunidade...",
        "no_discussion_link": "Link de discussão não encontrado"
    },
    "ru": {  # Русский
        "select_download_method": "Выберите способ загрузки",
        "no_download_options": "Нет доступных ссылок для загрузки для этой платформы. Пожалуйста, свяжитесь с администратором.",
        "no_valid_download_source": "Нет действительного источника загрузки",
        "download_from_github": "Скачать с GitHub",
        "download_from_lanzouyun": "Скачать с Lanzouyun",
        "source_github": "Репозиторий GitHub",
        "source_lanzouyun": "Хранилище Lanzouyun",
        "community_page_title": "Общий доступ сообщества",
        "suggestions_title": "Предложения функций и улучшений – Давайте строить вместе!",
        "suggestions_description": "Мы ценим каждый отзыв. Делитесь здесь своими предложениями, идеями улучшений или творческими работами. Пожалуйста, соблюдайте вежливость и дружелюбие.",
        "suggestions_button": "Перейти к обсуждениям →",
        "loading_community": "Загрузка информации сообщества...",
        "no_discussion_link": "Ссылка на обсуждение не найдена"
    }
}

# 需要处理的文件列表 (文件名, 语言代码)
files_to_process = [
    ("zh.json", "zh"),
    ("zh-TW.json", "zh-TW"),
    ("en.json", "en"),
    ("de.json", "de"),
    ("fr.json", "fr"),
    ("ja.json", "ja"),
    ("ko.json", "ko"),
    ("pt.json", "pt"),
    ("ru.json", "ru"),
]

def update_json_file(filepath, lang_code):
    """读取JSON文件，添加缺失的键，保存回原文件"""
    if not os.path.exists(filepath):
        print(f"⚠️ 文件不存在: {filepath}")
        return False
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    new_keys = additional_keys.get(lang_code, additional_keys.get("zh", {}))
    added = []
    for key, value in new_keys.items():
        if key not in data:
            data[key] = value
            added.append(key)
    if added:
        # 保存时保持缩进2空格，并按字母顺序排序键（可选，但保持整洁）
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=False)
            f.write("\n")  # 末尾换行
        print(f"✅ 已更新: {filepath} 添加了 {len(added)} 个键")
        return True
    else:
        print(f"ℹ️ 无需更新: {filepath}")
        return True

def main():
    # 获取脚本所在目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(script_dir)
    print(f"📁 当前工作目录: {os.getcwd()}")
    print("=" * 50)
    for filename, lang_code in files_to_process:
        print(f"\n📄 处理文件: {filename}")
        update_json_file(filename, lang_code)
    print("\n" + "=" * 50)
    print("🎉 所有语言文件处理完成！")

if __name__ == "__main__":
    main()
