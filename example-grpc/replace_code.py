import os

def is_text_readable(file_path: str, chunk_size: int = 1024) -> bool:
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            file.read(chunk_size)
        return True
    except (UnicodeDecodeError, IOError):
        return False

def list_files(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            file_path = os.path.join(root, file)
            if is_text_readable(file_path):
                print(f'Processing file: {file_path}')
                with open(file_path, 'r') as f:
                    content = f.read()
                content = content.replace('@javax.annotation.Generated', '@javax.annotation.processing.Generated')
                with open(file_path, 'w') as f:
                    f.write(content)

directory = './src'

list_files(directory)
