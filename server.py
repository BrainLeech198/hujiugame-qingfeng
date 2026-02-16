import http.server
import socketserver
import webbrowser
import os

PORT = 8000

# 设置工作目录为脚本所在目录
os.chdir(os.path.dirname(os.path.abspath(__file__)))

Handler = http.server.SimpleHTTPRequestHandler

with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print(f"🚀 服务器已启动：http://localhost:{PORT}")
    print("📂 请确保 index.html 和其他文件位于当前目录")
    print("⏳ 正在自动打开浏览器...")
    
    # 自动打开首页
    webbrowser.open(f"http://localhost:{PORT}/index.html")
    
    print("🛑 按 Ctrl+C 停止服务器")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n👋 服务器已停止")
