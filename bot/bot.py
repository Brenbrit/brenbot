import discord
import os
import platform
import subprocess

#used in the @ parts and at the client.run line
client = discord.Client()
currentPlatform = platform.system().lower()

def getToken():
    
    if currentPlatform.startswith("linux"):
        loc = "/home/ubuntu/Brenbot/bot/codes.txt"
    elif currentPlatform.startswith("win"):
        loc = "C:\\Users\\Brenbrit\\Documents\\Brenbot\\bot\\codes.txt"
    file = open(loc, "r")
    return file.readline().split(":")[1]
    
        

@client.event
async def on_ready():
    print('We have logged in as {0.user}'.format(client))

@client.event
async def on_message(message):
    if message.author == client.user:
        return

    if message.content.lower().startswith("update"):
        await message.channel.send('ok')
        update()


#this actually starts the bot
token = getToken()
client.run(token)


def update():
    if currentPlatform.startswith("linux"):
        print("gotem")
        subprocess.run(["sh /home/ubuntu/update.sh"])
    
