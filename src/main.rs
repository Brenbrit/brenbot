use std::env::var;
use poise::serenity_prelude as serenity;
use regex::Regex;
#[macro_use]
extern crate lazy_static;

// Types used by all command functions
type Error = Box<dyn std::error::Error + Send + Sync>;
#[allow(unused)]
type Context<'a> = poise::Context<'a, Data, Error>;

lazy_static! {
    static ref X_COM: Regex = Regex::new("[^a-zA-Z\\d\\s:]*x\\.com").unwrap();
}

// Custom user data passed to all command functions
pub struct Data {
}

#[tokio::main]
async fn main() {
    env_logger::init();
    
    let token = var("DISCORD_TOKEN")
        .expect("Missing `DISCORD_TOKEN` env var.");

    let intents = serenity::GatewayIntents::non_privileged()
        | serenity::GatewayIntents::MESSAGE_CONTENT
        | serenity::GatewayIntents::GUILD_MESSAGES;

    let framework = poise::Framework::builder()
        .setup(move |_ctx, _ready, _framework| {
            Box::pin(async move {
                Ok(Data {})
            })
        })
        .options(poise::FrameworkOptions {
            event_handler: |ctx, event, framework, data| {
                Box::pin(event_handler(ctx, event, framework, data))
            },
            ..Default::default()
        })
        .build();

    let client = serenity::ClientBuilder::new(token, intents)
        .framework(framework)
        .await;

    client.unwrap().start().await.unwrap();
}

async fn event_handler(
    ctx: &serenity::Context,
    event: &serenity::FullEvent,
    _framework: poise::FrameworkContext<'_, Data, Error>,
    _data: &Data,
) -> Result<(), Error> {
    match event {
        serenity::FullEvent::Ready { data_about_bot, .. } => {
            println!("Logged in as {}{}", data_about_bot.user.name, match data_about_bot.user.discriminator {
                Some(discriminator) => { format!("#{}", discriminator) },
                None => "".to_string(),
            });
        }
        serenity::FullEvent::Message { new_message } => {

            // Don't read our own messages
            if ctx.cache.current_user().id == new_message.author.id {
                return Ok(());
            }

            let message_lower = new_message.content.to_lowercase();

            if X_COM.is_match(&message_lower) {
                // Message: @<user>: <twitter link>
                let to_send = format!("<@{}>: {}", new_message.author.id, new_message.content.replacen("x.com", "twitter.com", 1));
                let builder = serenity::CreateMessage::new()
                    .content(to_send);
                new_message.channel_id.send_message(&ctx.http, builder)
                    .await?;

                // Delete the old message
                new_message.delete(&ctx.http).await?;
            }
        }
        _ => {}
    }
    Ok(())
}